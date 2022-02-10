package dev.vgerasimov.templo

import dev.vgerasimov.templo.types.*

import org.scalacheck.Gen
import org.scalacheck.Prop.*

class ParserTest extends TemploTestSuite(Parser()):

    testOne("empty string -> empty list") { "" -> Nil }
    testOne("{} -> empty code block") { "{}" -> List(Block.Code("")) }
    testOne("{ code } -> non empty code block") { "{ code }" -> List(Block.Code(" code ")) } 
    testOne("text 1 { code } text 2 -> code arounded with text") { 
      "text 1 { code } text 2" -> List(Block.Text("text 1 "), Block.Code(" code "), Block.Text(" text 2")) 
    } 
    testOne("{ code 1 }{ code 2 }{ code 3 } -> 3 code blocks") {
        "{ code 1 }{ code 2 }{ code 3 }" -> List(Block.Code(" code 1 "), Block.Code(" code 2 "), Block.Code(" code 3 "))
    }