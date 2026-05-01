package dev.vgerasimov.templo

class TemploTest extends munit.FunSuite:

  test("render returns template unchanged when it has no expressions") {
    val result = Templo.render("plain text")
    assertEquals(result, Right("plain text"))
  }

  test("render can use preloaded data definitions") {
    val data = """
      |(def 'name "Alice")
      |(def 'age 42)
      |""".stripMargin

    val template = "name={{name}}, age={{age}}"

    val result = Templo.render(template, data)

    assertEquals(result, Right("name=Alice, age=42"))
  }

  test("render handles multiline JSON template with quoted keys") {
    val data = """
      |(def 'project_name "devcontainers-templo (Scala 3 + sbt)")
      |(def 'dockerfile_path "Dockerfile")
      |(def 'build_context "..")
      |(def 'remote_user "vscode")
      |(def 'devcontainer_dir ".devcontainer")
      |(def 'env_file_source "/home/admin/.env")
      |""".stripMargin

    val template =
      """{
        |  "name": "{{project_name}}",
        |  "build": {
        |    "dockerfile": "{{dockerfile_path}}",
        |    "context": "{{build_context}}"
        |  },
        |  "remoteUser": "{{remote_user}}",
        |  "mounts": [
        |    "source={{env_file_source}},target=/home/{{remote_user}}/.env,type=bind,consistency=cached,readonly"
        |  ],
        |  "postCreateCommand": "bash ${containerWorkspaceFolder}/{{devcontainer_dir}}/post-create.sh",
        |  "postStartCommand": "bash ${containerWorkspaceFolder}/{{devcontainer_dir}}/load-env.sh"
        |}
        |""".stripMargin

    val expected =
      """{
        |  "name": "devcontainers-templo (Scala 3 + sbt)",
        |  "build": {
        |    "dockerfile": "Dockerfile",
        |    "context": ".."
        |  },
        |  "remoteUser": "vscode",
        |  "mounts": [
        |    "source=/home/admin/.env,target=/home/vscode/.env,type=bind,consistency=cached,readonly"
        |  ],
        |  "postCreateCommand": "bash ${containerWorkspaceFolder}/.devcontainer/post-create.sh",
        |  "postStartCommand": "bash ${containerWorkspaceFolder}/.devcontainer/load-env.sh"
        |}
        |""".stripMargin

    val result = Templo.render(template, data)
    assertEquals(result, Right(expected))
  }
