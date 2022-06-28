package dev.vgerasimov.templo

class LizpInterpreterTest
    extends TemploTestSuite[String, String](input => Parser()(input).flatMap(LizpInterpreter.apply)):

  testOne("empty string -> empty string") { "" -> "" }
  testOne("simple plus operation should be processed") { """25 + 44 = {(+ 25 44)}""" ->  "25 + 44 = 69" }
