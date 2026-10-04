package dev.vgerasimov.templo

import dev.vgerasimov.slowparse.*
import dev.vgerasimov.slowparse.Parsers.{ *, given }

/** Minimal, project-owned YAML subset parser used by the CLI prelude.
  *
  * It deliberately keeps source as a character vector and parses indentation, mappings, sequences, and scalar grammar
  * structurally. This is not a YAML compatibility layer for arbitrary YAML documents.
  */
object YamlPrelude:
  private sealed trait Value
  private case object NullValue extends Value
  private case class BoolValue(value: Boolean) extends Value
  private case class NumberValue(rendered: String) extends Value
  private case class TextValue(value: String) extends Value
  private case class ObjectValue(values: Vector[(String, Value)]) extends Value
  private case class ArrayValue(values: Vector[Value]) extends Value

  private val sourceCharacters: P[List[Char]] = anyCharValue.rep()

  def parseToPrelude(yaml: String): Either[String, String] =
    sourceCharacters(yaml) match
      case POut.Success(characters, _, _, _) =>
        Document(Vector.from(characters)).parse() match
          case Left(message)            => Left(s"Cannot parse YAML data: $message")
          case Right(root: ObjectValue) => toPrelude(root)
          case Right(_)                 => Left("YAML data must be an object at the top level")
      case POut.Failure(message, _) => Left(s"Cannot read YAML data: $message")

  private def toPrelude(value: ObjectValue): Either[String, String] =
    val rendered = flatten(value).sortBy(_._1).map { case (name, yamlValue) =>
      toLizpValue(yamlValue).map(value => s"(def '${escapeSymbol(name)} $value)")
    }
    rendered
      .foldLeft(Right(Vector.empty[String]): Either[String, Vector[String]]) { (acc, next) =>
        for values <- acc; value <- next yield values :+ value
      }
      .map(_.mkString("\n"))

  private def toLizpValue(value: Value): Either[String, String] = value match
    case NullValue          => Right("nil")
    case BoolValue(value)   => Right(if value then "true" else "false")
    case NumberValue(value) => Right(value)
    case TextValue(value)   => Right(s"\"${escapeString(value)}\"")
    case _: ArrayValue      => Left("YAML arrays cannot be rendered as direct Lizp scalar values")
    case _: ObjectValue     => Left("YAML objects cannot be rendered as direct Lizp scalar values")

  private def flatten(value: Value): Vector[(String, Value)] =
    def visit(prefix: String, current: Value): Vector[(String, Value)] = current match
      case ObjectValue(values) =>
        values.flatMap { case (key, child) =>
          val next = if prefix == "" then key else s"${prefix}_$key"
          visit(next, child)
        }
      case ArrayValue(values) =>
        values.zipWithIndex.flatMap { case (child, index) =>
          val next = if prefix == "" then s"item_$index" else s"${prefix}_$index"
          visit(next, child)
        }
      case scalar => if prefix == "" then Vector.empty else Vector(prefix -> scalar)
    visit("", value)

  private def escapeString(value: String): String =
    value.toVector.flatMap {
      case '\\' => Vector('\\', '\\')
      case '"'  => Vector('\\', '"')
      case '\r' => Vector('\\', 'r')
      case '\n' => Vector('\\', 'n')
      case '\t' => Vector('\\', 't')
      case char => Vector(char)
    }.mkString

  private def escapeSymbol(value: String): String =
    value.toVector.map {
      case '\\' | '\'' | '"' => '_'
      case char              => char
    }.mkString

  private final class Document(source: Vector[Char]):
    private var offset = 0

    def parse(): Either[String, Value] =
      skipIgnorableLines()
      if lineIsDocumentMarker('-', '-', '-') then consumeLine()
      skipIgnorableLines()
      parseNode(0).flatMap { value =>
        skipIgnorableLines()
        if lineIsDocumentMarker('.', '.', '.') then
          consumeLine()
          skipIgnorableLines()
        if offset == source.size then Right(value)
        else Left("trailing content after YAML document")
      }

    private def parseNode(indent: Int): Either[String, Value] =
      if offset >= source.size then Left("expected YAML value")
      else if lineIndent() != indent then Left("unexpected indentation")
      else if atSequenceMarker(indent) then parseArray(indent)
      else parseObject(indent)

    private def parseObject(indent: Int): Either[String, Value] =
      val entries = Vector.newBuilder[(String, Value)]
      var continue = true
      while continue && offset < source.size && lineIndent() == indent && !atSequenceMarker(
          indent
        ) && !lineIsDocumentMarker('.', '.', '.')
      do
        parseMappingLine(indent) match
          case Left(error) => return Left(error)
          case Right((key, content)) =>
            if content == Vector('|') || content == Vector('>') then
              parseBlockScalar(content.head, indent) match
                case Left(error)  => return Left(error)
                case Right(value) => entries += key -> value
            else if content.nonEmpty then
              parseScalar(content) match
                case Left(error)  => return Left(error)
                case Right(value) => entries += key -> value
            else
              skipIgnorableLines()
              if offset >= source.size || lineIndent() <= indent then return Left("expected indented value")
              parseNode(lineIndent()) match
                case Left(error)  => return Left(error)
                case Right(value) => entries += key -> value
        skipIgnorableLines()
      Right(ObjectValue(entries.result()))

    private def parseArray(indent: Int): Either[String, Value] =
      val values = Vector.newBuilder[Value]
      var continue = true
      while continue && offset < source.size && lineIndent() == indent && atSequenceMarker(
          indent
        ) && !lineIsDocumentMarker('.', '.', '.')
      do
        val content = consumeSequenceLine(indent)
        if content == Vector('|') || content == Vector('>') then
          parseBlockScalar(content.head, indent) match
            case Left(error)  => return Left(error)
            case Right(value) => values += value
        else if content.nonEmpty then
          parseScalar(content) match
            case Left(error)  => return Left(error)
            case Right(value) => values += value
        else
          skipIgnorableLines()
          if offset >= source.size || lineIndent() <= indent then return Left("expected indented array value")
          parseNode(lineIndent()) match
            case Left(error)  => return Left(error)
            case Right(value) => values += value
        skipIgnorableLines()
      Right(ArrayValue(values.result()))

    private def parseMappingLine(indent: Int): Either[String, (String, Vector[Char])] =
      offset += indent
      val keyStart = offset
      while offset < source.size && source(offset) != ':' && source(offset) != '\n' do offset += 1
      if offset >= source.size || source(offset) != ':' then Left("expected ':' in mapping")
      else
        val key = trim(source.slice(keyStart, offset))
        if key.isEmpty then Left("mapping key cannot be empty")
        else
          offset += 1
          val contentStart = offset
          consumeLine()
          Right(key.mkString -> withoutComment(trim(source.slice(contentStart, offsetBeforeNewline))))

    private def consumeSequenceLine(indent: Int): Vector[Char] =
      offset += indent + 1
      if offset < source.size && source(offset) == ' ' then offset += 1
      val start = offset
      consumeLine()
      withoutComment(trim(source.slice(start, offsetBeforeNewline)))

    private def parseScalar(chars: Vector[Char]): Either[String, Value] =
      if chars == Vector('n', 'u', 'l', 'l') || chars == Vector('~') then Right(NullValue)
      else if chars == Vector('t', 'r', 'u', 'e') then Right(BoolValue(true))
      else if chars == Vector('f', 'a', 'l', 's', 'e') then Right(BoolValue(false))
      else if chars.headOption.contains('{') then flowObject(chars)
      else if chars.headOption.contains('[') then flowArray(chars)
      else if chars.headOption.contains('"') then quoted(chars, '"').map(TextValue.apply)
      else if chars.headOption.contains('\'') then quoted(chars, '\'').map(TextValue.apply)
      else if isNumber(chars) then Right(NumberValue(chars.mkString))
      else Right(TextValue(chars.mkString))

    private def parseBlockScalar(style: Char, parentIndent: Int): Either[String, Value] =
      val lines = Vector.newBuilder[Vector[Char]]
      while offset < source.size && !lineIsBlank() && lineIndent() > parentIndent do
        val indentation = lineIndent()
        offset += indentation
        val start = offset
        consumeLine()
        lines += source.slice(start, offsetBeforeNewline)
      val separator = if style == '|' then Vector('\n') else Vector(' ')
      Right(TextValue(lines.result().flatMap(line => line ++ separator).dropRight(1).mkString))

    private def flowObject(chars: Vector[Char]): Either[String, Value] =
      val parser = Flow(chars)
      parser.objectValue().flatMap { value =>
        if parser.finished then Right(value) else Left("trailing flow mapping content")
      }

    private def flowArray(chars: Vector[Char]): Either[String, Value] =
      val parser = Flow(chars)
      parser.arrayValue().flatMap { value =>
        if parser.finished then Right(value) else Left("trailing flow sequence content")
      }

    private final class Flow(chars: Vector[Char]):
      private var index = 0

      def finished: Boolean =
        skipWhitespace()
        index == chars.size

      def objectValue(): Either[String, Value] =
        if !consume('{') then Left("expected '{' in flow mapping")
        else
          val entries = Vector.newBuilder[(String, Value)]
          skipWhitespace()
          while !peek('}') do
            key().flatMap { name =>
              if !consume(':') then Left("expected ':' in flow mapping")
              else value().map(parsed => entries += name -> parsed)
            } match
              case Left(error) => return Left(error)
              case Right(_)    => ()
            skipWhitespace()
            if !peek('}') && !consume(',') then return Left("expected ',' in flow mapping")
            skipWhitespace()
          consume('}')
          Right(ObjectValue(entries.result()))

      def arrayValue(): Either[String, Value] =
        if !consume('[') then Left("expected '[' in flow sequence")
        else
          val values = Vector.newBuilder[Value]
          skipWhitespace()
          while !peek(']') do
            value() match
              case Left(error)   => return Left(error)
              case Right(parsed) => values += parsed
            skipWhitespace()
            if !peek(']') && !consume(',') then return Left("expected ',' in flow sequence")
            skipWhitespace()
          consume(']')
          Right(ArrayValue(values.result()))

      private def key(): Either[String, String] =
        token(':', '}').flatMap { raw =>
          if raw.headOption.contains('"') || raw.headOption.contains('\'') then quoted(raw, raw.head).map(identity)
          else if raw.isEmpty then Left("flow mapping key cannot be empty")
          else Right(raw.mkString)
        }

      private def value(): Either[String, Value] =
        skipWhitespace()
        if peek('{') then objectValue()
        else if peek('[') then arrayValue()
        else token(',', '}', ']').flatMap(parseScalar)

      private def token(stops: Char*): Either[String, Vector[Char]] =
        skipWhitespace()
        val start = index
        var quote: Option[Char] = None
        var escaped = false
        while index < chars.size && (quote.nonEmpty || !stops.contains(chars(index))) do
          val char = chars(index)
          quote match
            case Some('"') if escaped                => escaped = false
            case Some('"') if char == '\\'           => escaped = true
            case Some(current) if char == current    => quote = None
            case None if char == '"' || char == '\'' => quote = Some(char)
            case _                                   => ()
          index += 1
        if quote.nonEmpty then Left("unterminated quoted flow scalar")
        else Right(trim(chars.slice(start, index)))

      private def consume(char: Char): Boolean =
        skipWhitespace()
        if peek(char) then
          index += 1
          true
        else false

      private def peek(char: Char): Boolean = index < chars.size && chars(index) == char

      private def skipWhitespace(): Unit =
        while index < chars.size && whitespace(chars(index)) do index += 1

    private def quoted(chars: Vector[Char], quote: Char): Either[String, String] =
      if chars.size < 2 || chars.last != quote then Left("unterminated quoted scalar")
      else
        val body = chars.slice(1, chars.size - 1)
        if quote == '\'' then Right(body.mkString)
        else
          val output = Vector.newBuilder[Char]
          var i = 0
          while i < body.size do
            if body(i) == '\\' then
              if i + 1 >= body.size then return Left("unterminated escape sequence")
              body(i + 1) match
                case 'n'   => output += '\n'
                case 'r'   => output += '\r'
                case 't'   => output += '\t'
                case '"'   => output += '"'
                case '\\'  => output += '\\'
                case other => output += other
              i += 2
            else
              output += body(i)
              i += 1
          Right(output.result().mkString)

    private def isNumber(chars: Vector[Char]): Boolean =
      var index = 0
      if chars.headOption.contains('-') || chars.headOption.contains('+') then index += 1
      val digitsStart = index
      while index < chars.size && isDigit(chars(index)) do index += 1
      val integral = index > digitsStart
      if index < chars.size && chars(index) == '.' then
        index += 1
        val fractionStart = index
        while index < chars.size && isDigit(chars(index)) do index += 1
        integral && index > fractionStart && index == chars.size
      else integral && index == chars.size

    private def isDigit(char: Char): Boolean = char >= '0' && char <= '9'

    private def lineIndent(): Int =
      var index = offset
      var count = 0
      while index < source.size && source(index) == ' ' do
        index += 1
        count += 1
      count

    private def atSequenceMarker(indent: Int): Boolean =
      offset + indent < source.size && source(offset + indent) == '-' &&
      (offset + indent + 1 >= source.size || source(offset + indent + 1) == ' ' || source(offset + indent + 1) == '\n')

    private def consumeLine(): Unit =
      while offset < source.size && source(offset) != '\n' do offset += 1
      offsetBeforeNewline = offset
      if offset < source.size then offset += 1

    private var offsetBeforeNewline = 0

    private def skipIgnorableLines(): Unit =
      while offset < source.size && (lineIsBlank() || lineIsComment()) do consumeLine()

    private def lineIsBlank(): Boolean =
      var index = offset
      while index < source.size && (source(index) == ' ' || source(index) == '\t' || source(index) == '\r') do
        index += 1
      index >= source.size || source(index) == '\n'

    private def lineIsComment(): Boolean =
      var index = offset
      while index < source.size && (source(index) == ' ' || source(index) == '\t') do index += 1
      index < source.size && source(index) == '#'

    private def lineIsDocumentMarker(first: Char, second: Char, third: Char): Boolean =
      val start = offset + lineIndent()
      start + 3 <= source.size && source.slice(start, start + 3) == Vector(first, second, third) &&
      (start + 3 == source.size || source(start + 3) == '\n' || source(start + 3) == ' ' || source(start + 3) == '#')

    private def withoutComment(chars: Vector[Char]): Vector[Char] =
      var quoted: Option[Char] = None
      var escaped = false
      var index = 0
      while index < chars.size do
        val char = chars(index)
        quoted match
          case Some(quote) if quote == '"' && escaped      => escaped = false
          case Some(quote) if quote == '"' && char == '\\' => escaped = true
          case Some(quote) if char == quote                => quoted = None
          case Some(_)                                     => ()
          case None if char == '\'' || char == '"'         => quoted = Some(char)
          case None if char == '#'                         => return trim(chars.slice(0, index))
          case None                                        => ()
        index += 1
      chars

    private def trim(chars: Vector[Char]): Vector[Char] =
      var start = 0
      var end = chars.size
      while start < end && whitespace(chars(start)) do start += 1
      while end > start && whitespace(chars(end - 1)) do end -= 1
      chars.slice(start, end)

    private def whitespace(char: Char): Boolean = char == ' ' || char == '\t' || char == '\r'
