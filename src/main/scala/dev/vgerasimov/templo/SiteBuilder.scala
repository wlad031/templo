package dev.vgerasimov.templo

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import scala.jdk.CollectionConverters.*
import scala.util.Using
import scala.util.Try

/** Builds a static output tree from templates and assets.
  *
  * Files ending in `.tmpl` are rendered with one optional site-wide data file and written without that extension. Other
  * files are copied unchanged. Output is staged beside its destination and installed only after every source file
  * succeeds.
  */
object SiteBuilder:
  final case class Config(source: Path, output: Path, data: Option[Path], force: Boolean)

  def build(config: Config): Either[String, Unit] =
    val source = config.source.toAbsolutePath.normalize
    val output = config.output.toAbsolutePath.normalize
    for
      _       <- validate(source, output, config.force)
      prelude <- loadPrelude(config.data)
      _       <- stageAndInstall(source, output, prelude)
    yield ()

  private def validate(source: Path, output: Path, force: Boolean): Either[String, Unit] =
    if !Files.isDirectory(source) then Left(s"Site source directory does not exist: $source")
    else if output.startsWith(source) then Left("Site output directory must not be inside its source directory")
    else if Files.exists(output) && !force then
      Left(s"Site output directory already exists: $output (use --force to replace it)")
    else Right(())

  private def loadPrelude(data: Option[Path]): Either[String, String] =
    data match
      case None => Right("")
      case Some(path) =>
        readText(path).flatMap { text =>
          if isYaml(path) then YamlPrelude.parseToPrelude(text) else Right(text)
        }

  private def stageAndInstall(source: Path, output: Path, prelude: String): Either[String, Unit] =
    val parent = Option(output.getParent).getOrElse(Path.of(".").toAbsolutePath)
    Try(Files.createDirectories(parent)).toEither.left
      .map(error => s"Cannot create site output parent: ${error.getMessage}")
      .flatMap { _ =>
        Try(Files.createTempDirectory(parent, ".templo-site-")).toEither.left
          .map(error => s"Cannot stage site output: ${error.getMessage}")
          .flatMap { staging =>
            renderTree(source, staging, prelude) match
              case Left(error) =>
                deleteTree(staging)
                Left(error)
              case Right(()) =>
                if Files.exists(output) then deleteTree(output)
                Try(Files.move(staging, output, StandardCopyOption.ATOMIC_MOVE)).recoverWith { case _ =>
                  Try(Files.move(staging, output))
                }.toEither.left
                  .map(error => s"Cannot install site output: ${error.getMessage}")
                  .map(_ => ())
          }
      }

  private def renderTree(source: Path, staging: Path, prelude: String): Either[String, Unit] =
    val paths = Using.resource(Files.walk(source))(_.iterator.asScala.toVector.sortBy(_.toString))
    paths.foldLeft[Either[String, Unit]](Right(())) { (result, path) =>
      result.flatMap { _ =>
        if Files.isDirectory(path) then Right(())
        else renderFile(source, staging, path, prelude)
      }
    }

  private def renderFile(source: Path, staging: Path, path: Path, prelude: String): Either[String, Unit] =
    val relative = source.relativize(path)
    val destination = staging.resolve(outputRelative(relative))
    Try(Files.createDirectories(destination.getParent)).toEither.left
      .map(error => s"Cannot create site directory: ${error.getMessage}")
      .flatMap { _ =>
        if isTemplate(path) then
          readText(path)
            .flatMap(template => Templo.render(template, prelude).left.map(_.toString))
            .flatMap(rendered => writeText(destination, rendered))
        else
          Try(Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING)).toEither.left
            .map(error => s"Cannot copy site asset '$relative': ${error.getMessage}")
            .map(_ => ())
      }

  private def outputRelative(relative: Path): Path =
    val name = relative.getFileName.toString
    if name.endsWith(".tmpl") then relative.resolveSibling(name.dropRight(".tmpl".length)) else relative

  private def isTemplate(path: Path): Boolean = path.getFileName.toString.endsWith(".tmpl")

  private def isYaml(path: Path): Boolean =
    val name = path.getFileName.toString.toLowerCase
    name.endsWith(".yaml") || name.endsWith(".yml")

  private def readText(path: Path): Either[String, String] =
    Try(Files.readString(path)).toEither.left.map(error => s"Cannot read '$path': ${error.getMessage}")

  private def writeText(path: Path, content: String): Either[String, Unit] =
    Try(Files.writeString(path, content)).toEither.left
      .map(error => s"Cannot write '$path': ${error.getMessage}")
      .map(_ => ())

  private def deleteTree(path: Path): Unit =
    if Files.exists(path) then
      Using.resource(Files.walk(path))(_.iterator.asScala.toVector.sortBy(_.getNameCount).reverse.foreach(Files.delete))
