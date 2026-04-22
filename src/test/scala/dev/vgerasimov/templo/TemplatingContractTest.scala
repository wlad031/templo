package dev.vgerasimov.templo

class TemplatingContractTest
    extends TemploTestSuite[String, String](input => Parser()(input).flatMap(LizpInterpreter.apply)):

  testOne("renders plain text template unchanged") {
    "hello template" -> "hello template"
  }

  testOne("renders single expression block") {
    "{(+ 1 2)}" -> "3"
  }

  testOne("renders adjacent expression blocks") {
    "{(+ 1 1)}{(+ 2 2)}{(+ 3 3)}" -> "246"
  }

  testOne("interpolates expression between text segments") {
    "a{(+ 1 2)}b" -> "a3b"
  }

  testOne("interpolates multiple expression blocks in one line") {
    "x={(+ 1 2)}, y={(+ 4 5)}" -> "x=3, y=9"
  }

  testOne("preserves newlines around interpolated expressions") {
    "line1\n{(+ 1 2)}\nline3" -> "line1\n3\nline3"
  }

  testOne("handles empty code block inside text") {
    "left{}right" -> "leftright"
  }

  testOne("README double-brace envelope with two insertions") {
    "{{+ }{1}{2}}}" -> "12"
  }

  testOne("README style envelope with surrounding text and insertion") {
    "{{+ }hello {\"world\"}}}" -> "hello world"
  }

  testOne("README style envelope transforms text chunks into string args") {
    "{{+ }sum={(+ 1 2)}; done{}}}" -> "sum=3; done"
  }

  testOne("interpolates expression at beginning and end") {
    "{(+ 1 2)} middle {(+ 3 4)}" -> "3 middle 7"
  }

  testOne("supports repeated dynamic values") {
    "{(+ 2 3)}-{(+ 2 3)}-{(+ 2 3)}" -> "5-5-5"
  }

  testOne("treats multiline README envelope as a single template") {
    "{{+ }hello\nworld {\"!\"}}}" -> "hello world !"
  }
