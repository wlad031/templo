package dev.vgerasimov.templo

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files

class CliYamlIntegrationTest extends munit.FunSuite:

  test("cli renders template using yaml vars file") {
    val tempDir = Files.createTempDirectory("templo-yaml-it-")
    val templatePath = tempDir.resolve("template.tmpl")
    val yamlPath = tempDir.resolve("vars.yaml")

    val template = "user={{name}}, enabled={{enabled}}, retries={{retries}}, ratio={{ratio}}, note={{note}}"
    val yaml =
      """name: Alice
        |enabled: true
        |retries: 3
        |ratio: 2.5
        |note: ok
        |""".stripMargin

    Files.writeString(templatePath, template)
    Files.writeString(yamlPath, yaml)

    val renderedOut = ByteArrayOutputStream()
    val printed = Console.withOut(PrintStream(renderedOut)) {
      Main.main(Array(templatePath.toString, yamlPath.toString))
      renderedOut.toString(StandardCharsets.UTF_8)
    }

    assertEquals(printed, "user=Alice, enabled=true, retries=3, ratio=2.5, note=ok\n")
  }

  test("cli renders template using nested yaml vars and arrays") {
    val tempDir = Files.createTempDirectory("templo-yaml-it-nested-")
    val templatePath = tempDir.resolve("template-nested.tmpl")
    val yamlPath = tempDir.resolve("vars-nested.yaml")

    val template =
      "team={{project_team_name}}, env={{project_team_env_name}}, owner={{project_team_env_owner}}, first={{services_0}}, second={{services_1}}, third={{services_2}}"
    val yaml =
      """project:
        |  team:
        |    name: core
        |    env:
        |      name: prod
        |      owner: platform
        |services:
        |  - api
        |  - worker
        |  - web
        |""".stripMargin

    Files.writeString(templatePath, template)
    Files.writeString(yamlPath, yaml)

    val renderedOut = ByteArrayOutputStream()
    val printed = Console.withOut(PrintStream(renderedOut)) {
      Main.main(Array(templatePath.toString, yamlPath.toString))
      renderedOut.toString(StandardCharsets.UTF_8)
    }

    assertEquals(
      printed,
      "team=core, env=prod, owner=platform, first=api, second=worker, third=web\n"
    )
  }
