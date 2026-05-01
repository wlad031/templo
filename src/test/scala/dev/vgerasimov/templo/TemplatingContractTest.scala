package dev.vgerasimov.templo

class TemplatingContractTest
    extends TemploTestSuite[String, String](input => Parser()(input).flatMap(LizpInterpreter.apply)):

  testOne("renders plain text template unchanged") {
    "hello template" -> "hello template"
  }

  testOne("renders single expression block") {
    "{{(+ 1 2)}}" -> "3"
  }

  testOne("renders adjacent expression blocks") {
    "{{(+ 1 1)}}{{(+ 2 2)}}{{(+ 3 3)}}" -> "246"
  }

  testOne("interpolates expression between text segments") {
    "a{{(+ 1 2)}}b" -> "a3b"
  }

  testOne("interpolates multiple expression blocks in one line") {
    "x={{(+ 1 2)}}, y={{(+ 4 5)}}" -> "x=3, y=9"
  }

  testOne("preserves newlines around interpolated expressions") {
    "line1\n{{(+ 1 2)}}\nline3" -> "line1\n3\nline3"
  }

  testOne("handles empty code block inside text") {
    "left{{}}right" -> "leftright"
  }

  testOne("treats single-brace chunks as plain text") {
    "left{(+ 1 2)}right" -> "left{(+ 1 2)}right"
  }

  testOne("allows expression continuation across blocks with text inserted") {
    "{{(concat (+ 1 2)}} and {{(+ 3 4))}}" -> "3 and 7"
  }

  testOne("example cross-block expression from discussion") {
    "{{(concat }} text {{ )}}" -> " text "
  }

  testOne("interpolates expression at beginning and end") {
    "{{(+ 1 2)}} middle {{(+ 3 4)}}" -> "3 middle 7"
  }

  testOne("supports repeated dynamic values") {
    "{{(+ 2 3)}}-{{(+ 2 3)}}-{{(+ 2 3)}}" -> "5-5-5"
  }

  testOne("preserves multiline text between expressions") {
    "{{(+ 1 2)}}\nhello\n{{(+ 3 4)}}" -> "3\nhello\n7"
  }
