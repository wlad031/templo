package dev.vgerasimov.templo

import dev.vgerasimov.templo.types.*
import dev.vgerasimov.templo.syntax.*
import dev.vgerasimov.slowparse.*
import dev.vgerasimov.slowparse.Parsers.{ *, given }

/** Parses template into text/code blocks or [[Parser.Error]]. */
trait Parser extends (String => Either[Parser.Error, List[Block]])

/** Contains parser-related types and functions. */
object Parser:

  /** Returns new [[Parser]]. */
  def apply(): Parser = SlowparseParser

  case class Error(message: String) extends TemploError

/** [[Parser]] implementation using [[https://github.com/wlad031/slowparse Slowparse]] library. */
object SlowparseParser extends Parser:

  private val escapedStringChar: P[String] = (P("\\") ~ anyChar).!
  private val plainStringChar: P[String] = (!P("\"") ~ anyChar).!
  private val quotedString: P[String] = (P("\"") ~ (escapedStringChar | plainStringChar).rep() ~ P("\"")).!

  private val codeChunk: P[String] = quotedString | (!P("}}") ~ anyChar).!
  private val codeBlock: P[Block] = (P("{{") ~ codeChunk.rep().map(_.mkString) ~ P("}}")).map(Block.Code.apply)
  private val textCharBlock: P[Block] = (!P("{{") ~ anyChar).!.map(Block.Text.apply)
  private val blocks: P[List[Block]] = (codeBlock | textCharBlock).rep()

  override def apply(string: String): Either[Parser.Error, List[Block]] =
    blocks(string) match
      case POut.Success(parsed, _, remaining, _) if remaining.isEmpty =>
        mergeTextBlocks(parsed).asRight
      case POut.Success(_, _, remaining, _) if remaining.startsWith("{{") =>
        Parser.Error("Unclosed template expression").asLeft
      case POut.Success(_, _, remaining, _) =>
        Parser.Error(s"Could not parse template near: ${preview(remaining)}").asLeft
      case POut.Failure(message, _) =>
        Parser.Error(message).asLeft

  private def mergeTextBlocks(parsed: List[Block]): List[Block] =
    parsed
      .foldLeft(List.empty[Block]) { (acc, block) =>
        (acc, block) match
          case (Block.Text(prev) :: tail, Block.Text(current)) => Block.Text(s"$prev$current") :: tail
          case _                                                => block :: acc
      }
      .reverse

  private def preview(value: String): String =
    val size = 30
    if value.length <= size then value else s"${value.take(size)}..."
