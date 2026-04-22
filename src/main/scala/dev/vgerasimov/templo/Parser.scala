package dev.vgerasimov.templo

import dev.vgerasimov.templo.types.*
import dev.vgerasimov.templo.syntax.*

/** Parses given string into [[???]] or [[Parser.Error]] cannot be correctly parsed. */
trait Parser extends (String => Either[Parser.Error, List[Block]])

/** Contains parser-related types and functions. */
object Parser:

  /** Returns new [[Parser]]. */
  def apply(): Parser = SlowparseParser

  case class Error(message: String) extends TemploError

/** [[Parser]] implementation using [[https://github.com/wlad031/slowparse Slowparse]] library. */
object SlowparseParser extends Parser:
  override def apply(string: String): Either[Parser.Error, List[Block]] =
    parseBlocks(string)

  private def parseBlocks(input: String): Either[Parser.Error, List[Block]] =
    val blocks = scala.collection.mutable.ListBuffer.empty[Block]
    val text = new StringBuilder

    def flushText(): Unit =
      if text.nonEmpty then
        blocks += Block.Text(text.toString)
        text.clear()

    var i = 0
    while i < input.length do
      if input.charAt(i) == '{' then
        flushText()
        if i + 1 < input.length && input.charAt(i + 1) == '{' then
          parseEnvelope(input, i + 2) match
            case Left(error) => return error.asLeft
            case Right((content, next)) =>
              blocks += Block.Envelope(content)
              i = next
        else
          parseCode(input, i + 1) match
            case Left(error) => return error.asLeft
            case Right((content, next)) =>
              blocks += Block.Code(content)
              i = next
      else
        text.append(input.charAt(i))
        i += 1

    flushText()
    blocks.toList.asRight

  private def parseCode(input: String, from: Int): Either[Parser.Error, (String, Int)] =
    val out = new StringBuilder
    var i = from
    var depth = 1
    var inQuotes = false
    var escaped = false

    while i < input.length do
      val ch = input.charAt(i)
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
            if depth == 0 then return (out.toString, i + 1).asRight
            out.append(ch)
            i += 1
          case _ =>
            out.append(ch)
            i += 1

    Parser.Error("Unclosed code block").asLeft

  private def parseEnvelope(input: String, from: Int): Either[Parser.Error, (String, Int)] =
    val out = new StringBuilder
    var i = from
    var depth = 0
    var inQuotes = false
    var escaped = false

    while i < input.length do
      val ch = input.charAt(i)
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
            if depth > 0 then
              out.append(ch)
              depth -= 1
              i += 1
            else if i + 1 < input.length && input.charAt(i + 1) == '}' then
              return (out.toString, i + 2).asRight
            else
              out.append(ch)
              i += 1
          case _ =>
            out.append(ch)
            i += 1

    Parser.Error("Unclosed template envelope").asLeft
