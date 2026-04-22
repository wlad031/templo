package dev.vgerasimov.templo

import dev.vgerasimov.templo.types.*
import dev.vgerasimov.templo.syntax.*

trait Interpreter extends (List[Block] => Either[Interpreter.Error, String])

object Interpreter:
  case class Error(message: String) extends TemploError

object LizpInterpreter extends Interpreter:
  import dev.vgerasimov.lizp.{ Reader as KekR, Interpreter as KekI, Parser as KekP, * }

  override def apply(blocks: List[Block]): Either[Interpreter.Error, String] =
    val reader = KekR()
    val parser = KekP()

    val initialContext = KekI.Context(
      reader = reader,
      parser = parser,
      sourcePaths = Set(
        Source.resource("std.lz"),
        Source.directory(".")
      ),
      withNoNotes = true
    )

    blocks
      .foldLeft[Either[Interpreter.Error, StringBuilder]]((new StringBuilder).asRight) {
        case (acc, block) =>
          acc.flatMap { out =>
            block match
              case Block.Text(text) =>
                out.append(text)
                out.asRight
              case Block.Code(code) if code.trim.isEmpty =>
                out.asRight
              case Block.Code(code) =>
                evalCode(code, parser, initialContext).map { piece =>
                  out.append(piece)
                  out
                }
              case Block.Envelope(body) =>
                evalEnvelope(body, parser, initialContext).map { piece =>
                  out.append(piece)
                  out
                }
          }
      }
      .map(_.toString)

  private def evalCode(
      source: String,
      parser: KekP,
      context: KekI.Context
  ): Either[Interpreter.Error, String] =
    val wrapped = s"(include std)\n$source"
    (for
      parsed <- parser(wrapped)
      evaluated <-
        given KekI.Context = context
        KekI()(parsed)
    yield evaluated) match
      case Left(error) => Interpreter.Error(error.toString).asLeft
      case Right(value) =>
        val rendered = renderValue(value._1.toString)
        rendered.asRight

  private def renderValue(raw: String): String =
    val item = unwrapLastListElement(raw.trim)
    if item.startsWith("\"") && item.endsWith("\"") && item.length >= 2 then
      unescape(item.drop(1).dropRight(1))
    else item

  private def unwrapLastListElement(raw: String): String =
    if !(raw.startsWith("[") && raw.endsWith("]")) then raw
    else
      val inner = raw.drop(1).dropRight(1)
      val items = splitTopLevel(inner)
      if items.isEmpty then ""
      else unwrapLastListElement(items.last)

  private def splitTopLevel(source: String): List[String] =
    val result = scala.collection.mutable.ListBuffer.empty[String]
    val current = new StringBuilder
    var inQuotes = false
    var escaped = false
    var squareDepth = 0
    var roundDepth = 0

    var i = 0
    while i < source.length do
      val ch = source.charAt(i)
      if inQuotes then
        current.append(ch)
        if escaped then escaped = false
        else if ch == '\\' then escaped = true
        else if ch == '"' then inQuotes = false
      else
        ch match
          case '"' =>
            inQuotes = true
            current.append(ch)
          case '[' =>
            squareDepth += 1
            current.append(ch)
          case ']' =>
            squareDepth -= 1
            current.append(ch)
          case '(' =>
            roundDepth += 1
            current.append(ch)
          case ')' =>
            roundDepth -= 1
            current.append(ch)
          case ',' if squareDepth == 0 && roundDepth == 0 =>
            val value = current.toString.trim
            if value.nonEmpty then result += value
            current.clear()
          case _ =>
            current.append(ch)
      i += 1

    val tail = current.toString.trim
    if tail.nonEmpty then result += tail
    result.toList

  private def envelopeToCode(raw: String): Either[Interpreter.Error, String] =
    val (head, body) = splitEnvelope(raw)
    if head.trim.isEmpty then Interpreter.Error("Template envelope has empty head expression").asLeft
    else
      val args = envelopeArgs(body)
      val renderedArgs = args.mkString(" ")
      val code =
        if renderedArgs.isEmpty then s"(${head.trim})"
        else s"(${head.trim} $renderedArgs)"
      code.asRight

  private def evalEnvelope(
      raw: String,
      parser: KekP,
      context: KekI.Context
  ): Either[Interpreter.Error, String] =
    val (head, body) = splitEnvelope(raw)
    if head.trim == "+" then
      parseEnvelopeBody(body)
        .foldLeft[Either[Interpreter.Error, StringBuilder]]((new StringBuilder).asRight) {
          case (acc, EnvelopePart.Text(value)) =>
            acc.map { out =>
              out.append(value.replaceAll("\\s+", " "))
              out
            }
          case (acc, EnvelopePart.Expr(value)) =>
            val expr = value.trim
            if expr.isEmpty then acc
            else
              acc.flatMap(out => evalCode(expr, parser, context).map { piece =>
                out.append(piece)
                out
              })
        }
        .map(_.toString)
    else
      envelopeToCode(raw).flatMap(evalCode(_, parser, context))

  private def splitEnvelope(raw: String): (String, String) =
    var i = 0
    var inQuotes = false
    var escaped = false
    var depth = 0
    while i < raw.length do
      val ch = raw.charAt(i)
      if inQuotes then
        if escaped then escaped = false
        else if ch == '\\' then escaped = true
        else if ch == '"' then inQuotes = false
      else
        ch match
          case '"' => inQuotes = true
          case '{' => depth += 1
          case '}' if depth == 0 => return (raw.take(i), raw.drop(i + 1))
          case '}' => depth -= 1
          case _ => ()
      i += 1
    (raw, "")

  private enum EnvelopePart:
    case Text(value: String)
    case Expr(value: String)

  private def envelopeArgs(body: String): List[String] =
    val parsed = parseEnvelopeBody(body)
    parsed.flatMap {
      case EnvelopePart.Expr(value) =>
        val expr = value.trim
        if expr.isEmpty then Nil else List(expr)
      case EnvelopePart.Text(value) =>
        val normalized = value.replaceAll("\\s+", " ")
        if normalized.trim.isEmpty then Nil
        else List(s"\"${escape(normalized)}\"")
    }

  private def parseEnvelopeBody(body: String): List[EnvelopePart] =
    val out = scala.collection.mutable.ListBuffer.empty[EnvelopePart]
    val text = new StringBuilder

    def flushText(): Unit =
      if text.nonEmpty then
        out += EnvelopePart.Text(text.toString)
        text.clear()

    var i = 0
    while i < body.length do
      val ch = body.charAt(i)
      if ch == '{' then
        flushText()
        parseNestedExpr(body, i + 1) match
          case Some((expr, next)) =>
            out += EnvelopePart.Expr(expr)
            i = next
          case None =>
            text.append(ch)
            i += 1
      else
        text.append(ch)
        i += 1

    flushText()
    out.toList

  private def parseNestedExpr(body: String, from: Int): Option[(String, Int)] =
    val out = new StringBuilder
    var i = from
    var depth = 1
    var inQuotes = false
    var escaped = false

    while i < body.length do
      val ch = body.charAt(i)
      if inQuotes then
        out.append(ch)
        if escaped then escaped = false
        else if ch == '\\' then escaped = true
        else if ch == '"' then inQuotes = false
        i += 1
      else
        ch match
          case '"' =>
            out.append(ch)
            inQuotes = true
            i += 1
          case '{' =>
            out.append(ch)
            depth += 1
            i += 1
          case '}' =>
            depth -= 1
            if depth == 0 then return Some((out.toString, i + 1))
            out.append(ch)
            i += 1
          case _ =>
            out.append(ch)
            i += 1

    None

  private def escape(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

  private def unescape(value: String): String =
    value.replace("\\\"", "\"").replace("\\\\", "\\")
