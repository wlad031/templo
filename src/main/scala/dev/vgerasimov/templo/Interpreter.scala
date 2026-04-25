package dev.vgerasimov.templo

import dev.vgerasimov.templo.types.*
import dev.vgerasimov.templo.syntax.*

trait Interpreter extends (List[Block] => Either[Interpreter.Error, String])

object Interpreter:
  case class Error(message: String) extends TemploError

object LizpInterpreter extends Interpreter:
  import dev.vgerasimov.lizp.{ Reader as LizpReader, Interpreter as LizpEval, Parser as LizpParser, * }
  import dev.vgerasimov.lizp.types.{ Expr as LizpExpr, Str as LizpStr, List as LizpList, Nil as LizpNil, :: as LizpCons }

  def withPrelude(prelude: String): Interpreter =
    (blocks: List[Block]) => evaluate(blocks, prelude)

  override def apply(blocks: List[Block]): Either[Interpreter.Error, String] =
    evaluate(blocks, "")

  private def evaluate(blocks: List[Block], prelude: String): Either[Interpreter.Error, String] =
    val reader = LizpReader()
    val parser = LizpParser()

    val initialContext = LizpEval.Context(
      reader = reader,
      parser = parser,
      sourcePaths = Set(
        Source.resource("std.lz"),
        Source.directory(".")
      ),
      withNoNotes = true
    )

    val source = buildProgram(blocks, prelude)

    (for
      parsed <- parser(source)
      evaluated <-
        given LizpEval.Context = initialContext
        LizpEval()(parsed)
    yield evaluated) match
      case Left(error) => Interpreter.Error(error.toString).asLeft
      case Right(value) => renderValue(value._1).asRight

  private def buildProgram(blocks: List[Block], prelude: String): String =
    val args = blocks.flatMap {
      case Block.Text(text) if text.nonEmpty => List(s"\"${escape(text)}\"")
      case Block.Code(code) if code.trim.nonEmpty => List(code)
      case _ => Nil
    }

    val concatExpr =
      if args.isEmpty then "(concat \"\")"
      else s"(concat ${args.mkString(" ")})"

    val preludeCode =
      if prelude.trim.isEmpty then ""
      else s"$prelude\n"

    s"(include std)\n$preludeCode$concatExpr"

  private def renderValue(expr: LizpExpr): String =
    lastValue(expr) match
      case LizpStr(value) => value
      case LizpNil        => ""
      case other         => other.toString

  private def lastValue(expr: LizpExpr): LizpExpr = expr match
    case LizpNil            => LizpNil
    case list: LizpList[?]  => lastValue(lastListValue(list))
    case other            => other

  private def lastListValue(list: LizpList[?]): LizpExpr = list match
    case LizpNil                 => LizpNil
    case LizpCons(head, LizpNil) => head
    case LizpCons(_, tail)       => lastListValue(tail)

  private def escape(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")
