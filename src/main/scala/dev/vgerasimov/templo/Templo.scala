package dev.vgerasimov.templo

object Templo:
  def render(template: String, data: String = ""): Either[TemploError, String] =
    Parser()(template).flatMap { blocks =>
      LizpInterpreter.withPrelude(data)(blocks)
    }
