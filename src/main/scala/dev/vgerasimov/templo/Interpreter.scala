package dev.vgerasimov.templo

import dev.vgerasimov.templo.types.*
import dev.vgerasimov.templo.syntax.*

trait Interpreter extends (List[Block] => Either[Interpreter.Error, String])

object Interpreter:
  case class Error(message: String) extends TemploError

object LizpInterpreter extends Interpreter:
  import dev.vgerasimov.lizp.{ Reader as KekR, Interpreter as KekI, Parser as KekP, * }

  override def apply(blocks: List[Block]): Either[Interpreter.Error, String] =
    val source = (Block.Code("(include std)(+ ") :: (blocks ++ List(Block.Code(")")))).map {
      case Block.Code(code) => code
      case Block.Text(text) => s""""$text""""
    }.mkString(" ")
    println(s"source = $source")
    val reader = KekR()
    val parser = KekP()
    (for {
      parsed <- parser(source)
      context = KekI.Context(
        reader = reader,
        parser = parser,
        sourcePaths = Set(
          Source.resource("std.lz"),
          Source.directory(".")
        ),
        withNoNotes = true
      )
      evaluated <- {
        given KekI.Context = context
        KekI()(parsed)
      }
    } yield evaluated) match
      case Left(err) => Interpreter.Error(err.toString).asLeft
      case Right(res) => res._1.toString.asRight
