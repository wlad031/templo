package dev.vgerasimov.templo

trait TemploError

object syntax:
  extension [A](a: A)
    def asRight: Right[Nothing, A] = Right(a)
    def asLeft: Left[A, Nothing] = Left(a)
