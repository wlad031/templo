package dev.vgerasimov.templo

import java.nio.file.Files
import java.nio.file.Path
import scala.util.Using
import scala.util.Try

object Main:
  private case class Config(
      templatePath: Path,
      dataPath: Option[Path],
      outputPath: Option[Path]
  )

  def main(args: Array[String]): Unit =
    if args.toList == List("--help") || args.isEmpty then
      println(help)
    else run(args.toList) match
      case Left(error) =>
        System.err.println(error)
        System.exit(1)
      case Right(()) => ()

  private def run(args: List[String]): Either[String, Unit] =
    parseArgs(args)
      .flatMap(renderFromFiles)
      .flatMap(writeOutput)

  private def parseArgs(args: List[String]): Either[String, Config] =
    args match
      case Nil => Left(help)
      case template :: Nil =>
        Right(Config(Path.of(template), None, None))
      case template :: data :: Nil =>
        Right(Config(Path.of(template), Some(Path.of(data)), None))
      case template :: data :: output :: Nil =>
        Right(Config(Path.of(template), Some(Path.of(data)), Some(Path.of(output))))
      case _ =>
        parseFlaggedArgs(args)

  private def parseFlaggedArgs(args: List[String]): Either[String, Config] =
    def loop(
        rest: List[String],
        template: Option[Path],
        data: Option[Path],
        output: Option[Path]
    ): Either[String, Config] =
      rest match
        case Nil =>
          template match
            case Some(path) => Right(Config(path, data, output))
            case None => Left("Missing --template argument\n\n" + help)
        case "--template" :: value :: tail =>
          loop(tail, Some(Path.of(value)), data, output)
        case "--data" :: value :: tail =>
          loop(tail, template, Some(Path.of(value)), output)
        case "--out" :: value :: tail =>
          loop(tail, template, data, Some(Path.of(value)))
        case unknown :: _ =>
          Left(s"Unknown arguments: $unknown\n\n$help")

    loop(args, None, None, None)

  private def renderFromFiles(config: Config): Either[String, (Config, String)] =
    for
      template <- readFile(config.templatePath)
      data <- config.dataPath.map(readFile).getOrElse(Right(""))
      output <- Templo.render(template, data).left.map(_.toString)
    yield (config, output)

  private def writeOutput(result: (Config, String)): Either[String, Unit] =
    result match
      case (config, output) =>
        config.outputPath match
          case Some(path) =>
            Try {
                val parent = path.getParent
                if parent != null then Files.createDirectories(parent)
                Files.writeString(path, output)
              }
              .map(_ => ())
              .toEither
              .left
              .map(err => s"Cannot write output file '$path': ${err.getMessage}")
          case None =>
            println(output)
            Right(())

  private def readFile(path: Path): Either[String, String] =
    Try {
        Using.resource(scala.io.Source.fromFile(path.toFile))(_.mkString)
      }
      .toEither
      .left
      .map(err => s"Cannot read file '$path': ${err.getMessage}")

  private val help =
    """Templo - template renderer
      |
      |Usage:
      |  templo <template-file> [data-file] [output-file]
      |  templo --template <template-file> [--data <data-file>] [--out <output-file>]
      |
      |When output file is omitted, rendered text is printed to stdout.
      |Data file should contain Lizp code prepended before template evaluation.
      |""".stripMargin
