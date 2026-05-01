package dev.vgerasimov.templo

class TemploTest extends munit.FunSuite:

  test("render returns template unchanged when it has no expressions") {
    val result = Templo.render("plain text")
    assertEquals(result, Right("plain text"))
  }

  test("render can use preloaded data definitions") {
    val data = """
      |(define name "Alice")
      |(define age 42)
      |""".stripMargin

    val template = "name={{name}}, age={{age}}"

    val result = Templo.render(template, data)

    assertEquals(result, Right("name=Alice, age=42"))
  }
