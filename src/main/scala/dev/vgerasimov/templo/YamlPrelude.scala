package dev.vgerasimov.templo

import dev.vgerasimov.slowparse.POut.{ Failure, Success }
import dev.vgerasimov.slowyaml4s.{ YArray, YBoolean, YNull, YNumber, YObject, YString, Yaml, parser }

object YamlPrelude:
  def parseToPrelude(yaml: String): Either[String, String] =
    parser(yaml) match
      case Failure(message, _) => Left(s"Cannot parse YAML data: $message")
      case Success(value, _, remaining, _) if remaining.trim.nonEmpty =>
        Left("Cannot parse YAML data: trailing content after YAML document")
      case Success(value, _, _, _) => toPrelude(value)

  private def toPrelude(value: Yaml): Either[String, String] =
    value match
      case YObject(values) =>
        flatten(YObject(values))
          .sortBy(_._1)
          .map { case (name, yamlValue) =>
            toLizpValue(yamlValue).map(rendered => s"(def '${escapeSymbol(name)} $rendered)")
          }
          .foldLeft(Right(List.empty[String]): Either[String, List[String]]) { (acc, next) =>
            for
              lines <- acc
              line <- next
            yield lines :+ line
          }
          .map(_.mkString("\n"))
      case _ => Left("YAML data must be an object at the top level")

  private def toLizpValue(value: Yaml): Either[String, String] =
    value match
      case YNull       => Right("nil")
      case YBoolean(v) => Right(v.toString)
      case YNumber(v) if v.isWhole => Right(v.toLong.toString)
      case YNumber(v)  => Right(v.toString)
      case YString(v)  => Right(s"\"${escapeString(v)}\"")
      case YArray(_)   => Left("YAML arrays cannot be rendered as direct Lizp scalar values")
      case YObject(_)  => Left("YAML objects cannot be rendered as direct Lizp scalar values")

  private def flatten(value: Yaml): List[(String, Yaml)] =
    def go(prefix: String, current: Yaml): List[(String, Yaml)] =
      current match
        case o: YObject =>
          o.v.toList.flatMap { case (k, v) =>
            val next = if prefix.isEmpty then k else s"${prefix}_$k"
            go(next, v)
          }
        case a: YArray =>
          a.v.zipWithIndex.flatMap { case (v, i) =>
            val next = if prefix.isEmpty then s"item_$i" else s"${prefix}_$i"
            go(next, v)
          }
        case scalar =>
          if prefix.isEmpty then Nil else List(prefix -> scalar)

    go("", value)

  private def escapeString(value: String): String =
    value
      .replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\r", "\\r")
      .replace("\n", "\\n")
      .replace("\t", "\\t")

  private def escapeSymbol(value: String): String =
    value
      .replace("\\", "_")
      .replace("'", "_")
      .replace("\"", "_")
