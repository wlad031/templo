package dev.vgerasimov.templo

import java.nio.file.Files

class SiteBuilderTest extends munit.FunSuite:

  test("site build renders templates, copies assets, and replaces output atomically") {
    val root = Files.createTempDirectory("templo-site-")
    val source = root.resolve("source")
    val output = root.resolve("public")
    Files.createDirectories(source.resolve("assets"))
    Files.writeString(source.resolve("index.html.tmpl"), "Hello {{name}}")
    Files.writeString(source.resolve("assets/site.css"), "body { color: black; }")
    Files.writeString(root.resolve("data.yaml"), "name: Alice\n")

    val built = SiteBuilder.build(SiteBuilder.Config(source, output, Some(root.resolve("data.yaml")), force = false))

    assertEquals(built, Right(()))
    assertEquals(Files.readString(output.resolve("index.html")), "Hello Alice")
    assertEquals(Files.readString(output.resolve("assets/site.css")), "body { color: black; }")
  }

  test("site build CLI renders an output tree") {
    val root = Files.createTempDirectory("templo-site-cli-")
    val source = root.resolve("source")
    val output = root.resolve("public")
    Files.createDirectories(source)
    Files.writeString(source.resolve("index.html.tmpl"), "Welcome")

    Main.main(Array("build", "--source", source.toString, "--out", output.toString))

    assertEquals(Files.readString(output.resolve("index.html")), "Welcome")
  }

  test("site build refuses existing output without force") {
    val root = Files.createTempDirectory("templo-site-existing-")
    val source = root.resolve("source")
    val output = root.resolve("public")
    Files.createDirectories(source)
    Files.createDirectories(output)

    val result = SiteBuilder.build(SiteBuilder.Config(source, output, None, force = false))

    assert(result.left.exists(_.contains("already exists")))
  }

  test("site build rejects output within source") {
    val source = Files.createTempDirectory("templo-site-nested-")

    val result = SiteBuilder.build(SiteBuilder.Config(source, source.resolve("public"), None, force = false))

    assert(result.left.exists(_.contains("must not be inside")))
  }
