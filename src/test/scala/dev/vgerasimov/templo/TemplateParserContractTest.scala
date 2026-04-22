package dev.vgerasimov.templo

import dev.vgerasimov.templo.types.*

class TemplateParserContractTest extends TemploTestSuite(Parser()):

  testOne("plain text should remain a single text block") {
    "just text" -> List(Block.Text("just text"))
  }

  testOne("single expression should become one code block") {
    "{(+ 1 2)}" -> List(Block.Code("(+ 1 2)"))
  }

  testOne("empty code block is preserved as empty code") {
    "{}" -> List(Block.Code(""))
  }

  testOne("adjacent code blocks are parsed in order") {
    "{a}{b}{c}" -> List(Block.Code("a"), Block.Code("b"), Block.Code("c"))
  }

  testOne("text around one code block becomes text code text") {
    "left {mid} right" -> List(Block.Text("left "), Block.Code("mid"), Block.Text(" right"))
  }

  testOne("text around two code blocks keeps exact segmentation") {
    "a{b}c{d}e" -> List(Block.Text("a"), Block.Code("b"), Block.Text("c"), Block.Code("d"), Block.Text("e"))
  }

  testOne("whitespace inside code block is preserved") {
    "{  spaced  }" -> List(Block.Code("  spaced  "))
  }

  testOne("newlines inside code block are preserved") {
    "{line1\nline2}" -> List(Block.Code("line1\nline2"))
  }

  testOne("leading and trailing text with empty code block") {
    "begin{}end" -> List(Block.Text("begin"), Block.Code(""), Block.Text("end"))
  }

  testOne("README style outer braces are parsed as one code block") {
    "{{+ {1} {2}}}" -> List(Block.Envelope("+ {1} {2}"))
  }

  testOne("multiple newlines around code are preserved in text") {
    "x\n{y}\nz" -> List(Block.Text("x\n"), Block.Code("y"), Block.Text("\nz"))
  }

  testOne("code block may contain quoted braces as plain chars") {
    "{\"{literal}\"}" -> List(Block.Code("\"{literal}\""))
  }
