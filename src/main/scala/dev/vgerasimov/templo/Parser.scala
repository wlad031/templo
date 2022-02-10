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

  import dev.vgerasimov.slowparse.*
  import dev.vgerasimov.slowparse.Parsers.{ *, given }

  override def apply(string: String): Either[Parser.Error, List[Block]] = parser(string) match
    case POut.Success(result, _, _, _) => result.asRight
    case POut.Failure(message, _)      => Parser.Error(message).asLeft
  
  private val parser: P[List[Block]] = P(codeBlock | textBlock).*

  private val codeBlock: P[Block.Code] = P("{") ~ until(P("}")).!.map(Block.Code.apply) ~ P("}")
  private val textBlock: P[Block.Text] = until(P("{")).!.map(Block.Text.apply)
