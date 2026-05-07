package dev.vgerasimov.templo

class YamlPreludeTest extends munit.FunSuite:

  test("yaml prelude converts scalar object values") {
    val yaml =
      """name: Alice
        |enabled: true
        |retries: 3
        |ratio: 2.5
        |missing: null
        |""".stripMargin

    val prelude = YamlPrelude.parseToPrelude(yaml)
    assert(prelude.isRight)

    val rendered = Templo.render("{{name}}/{{enabled}}/{{retries}}/{{ratio}}/{{missing}}", prelude.toOption.get)
    assertEquals(rendered, Right("Alice/true/3/2.5/"))
  }

  test("yaml prelude rejects non-object top-level values") {
    val yaml = "- one\n- two\n"
    val prelude = YamlPrelude.parseToPrelude(yaml)
    assert(prelude.left.exists(_.contains("top level")))
  }

  test("yaml prelude flattens nested values and arrays") {
    val yaml =
      """nested:
        |  name: Alice
        |  level2:
        |    city: Paris
        |tags:
        |  - alpha
        |  - beta
        |""".stripMargin
    val prelude = YamlPrelude.parseToPrelude(yaml)
    assert(prelude.isRight)

    val rendered = Templo.render("{{nested_name}}/{{nested_level2_city}}/{{tags_0}}/{{tags_1}}", prelude.toOption.get)
    assertEquals(rendered, Right("Alice/Paris/alpha/beta"))
  }
