package dev.vgerasimov.templo
package types

sealed trait Block

object Block:
  case class Code(string: String) extends Block
  case class Text(string: String) extends Block
