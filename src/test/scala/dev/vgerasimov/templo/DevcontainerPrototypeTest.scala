package dev.vgerasimov.templo

import munit.FunSuite

class DevcontainerPrototypeTest extends FunSuite:

  private def resource(path: String): String =
    val stream = Option(getClass.getResourceAsStream(path)).getOrElse {
      fail(s"Missing resource: $path")
    }
    val source = scala.io.Source.fromInputStream(stream)
    try source.mkString
    finally
      source.close()
      stream.close()

  test("prototype includes devcontainer file") {
    val json = resource("/prototype/devcontainer/devcontainer.json")
    assert(json.contains("\"dockerfile\": \"Dockerfile\""))
    assert(json.contains("\"initializeCommand\": \"bash .devcontainer/pre-start.sh\""))
    assert(json.contains("\"postCreateCommand\": \"bash .devcontainer/post-create.sh\""))
  }

  test("prototype includes dockerfile") {
    val dockerfile = resource("/prototype/devcontainer/Dockerfile")
    assert(dockerfile.contains("FROM mcr.microsoft.com/devcontainers/java:1-17-bullseye"))
    assert(dockerfile.contains("USER ${USERNAME}"))
  }

  test("prototype includes pre and post scripts") {
    val pre = resource("/prototype/devcontainer/pre-start.sh")
    val post = resource("/prototype/devcontainer/post-create.sh")
    assert(pre.contains("set -euo pipefail"))
    assert(post.contains("set -euo pipefail"))
  }
