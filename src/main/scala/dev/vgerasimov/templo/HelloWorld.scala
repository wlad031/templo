package dev.vgerasimov.templo

object Hello:
  def apply(s: String): String = s"Hello $s!"

@main def run = println(Hello("world"))
