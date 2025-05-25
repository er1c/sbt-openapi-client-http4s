package io.github.er1c.openapi.parser

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
 * Tests for the OpenApiParser class to verify it correctly parses
 * OpenAPI specifications from files and strings.
 */
class OpenApiParserSpec extends AnyFlatSpec with Matchers {

  val parser = new OpenApiParser()

  "OpenApiParser" should "successfully parse the petstore specification" in {
    val specPath = getClass.getResource("/specs/petstore_3.0.4.yml").getPath
    val openApi = parser.parseFromFile(specPath)

    openApi should not be null
    openApi.getInfo.getTitle shouldBe "Swagger Petstore - OpenAPI 3.0"
    openApi.getInfo.getVersion shouldBe "1.0.12"

    // Verify paths
    val paths = openApi.getPaths
    paths should not be null
    paths.get("/pet") should not be null
    paths.get("/pet/findByStatus") should not be null

    // Verify components
    val components = openApi.getComponents
    components should not be null
    components.getSchemas.get("Pet") should not be null
    components.getSchemas.get("ApiResponse") should not be null
  }

  it should "successfully parse the cosmology specification" in {
    val specPath = getClass.getResource("/specs/cosmology.yml").getPath
    val openApi = parser.parseFromFile(specPath)

    openApi should not be null
    openApi.getInfo.getTitle shouldBe "cosmology"
    openApi.getInfo.getVersion shouldBe "1.0.0"

    // Verify paths
    val paths = openApi.getPaths
    paths should not be null
    paths.get("/celestialBodies") should not be null
    paths.get("/solarSystems") should not be null
    paths.get("/stars") should not be null

    // Verify components
    val components = openApi.getComponents
    components should not be null
    components.getSchemas.get("CelestialBody") should not be null
    components.getSchemas.get("Star") should not be null
    components.getSchemas.get("Galaxy") should not be null
  }

  it should "throw an exception when parsing an invalid specification" in {
    val invalidSpec = """
      |this is not valid yaml
      |openapi: 3.0.0
      |info:
      |  title: Invalid Spec
      |  version: 1.0.0
      |  @#$%!&*():
      |paths:
      |  /invalid: {
      |""".stripMargin

    an[IllegalArgumentException] should be thrownBy {
      parser.parseFromString(invalidSpec)
    }
  }
}
