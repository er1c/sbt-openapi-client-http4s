package io.github.er1c.openapi.parser.model

import io.github.er1c.openapi.parser.OpenApiParser
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.JavaConverters._

/**
 * Tests specifically for parsing complex schema structures in the cosmology specification.
 * This includes testing the proper parsing of discriminator, oneOf, allOf, and anyOf constructs.
 */
class CosmologySchemaSpec extends AnyFlatSpec with Matchers {

  val parser = new OpenApiParser()
  val modelGenerator = new ModelGenerator("io.github.er1c.test")
  val operationGenerator = new OperationGenerator("io.github.er1c.test")

  "ModelGenerator" should "correctly parse allOf schema in Moon (CelestialBody extension)" in {
    val specPath = getClass.getResource("/specs/cosmology.yml").getPath
    val openApi = parser.parseFromFile(specPath)

    val moonSchema = openApi.getComponents.getSchemas.get("Moon")
    moonSchema should not be null

    // Verify it's an allOf schema
    moonSchema.getAllOf should not be null
    moonSchema.getAllOf.size() should be(1)

    // Verify it extends CelestialBody
    val celestialBodyRef = moonSchema.getAllOf.get(0).get$ref()
    celestialBodyRef should not be null
    celestialBodyRef should include("CelestialBody")

    // Verify Moon has its own properties plus inherited ones
    moonSchema.getProperties.containsKey("uuid") should be(true)

    // Generate the model and check merged properties
    val models = modelGenerator.generateModels(openApi)
    val moonModel = models.find(_.name == "Moon").get

    // Moon should have uuid property
    moonModel.properties.map(_.name) should contain("uuid")
    // Moon should inherit mass property from CelestialBody
    moonModel.properties.map(_.name) should contain("mass")

    // Check types - mass should be BigDecimal
    val massProp = moonModel.properties.find(_.name == "mass").get
    massProp.typeName should be("BigDecimal")

    // Check required fields
    val uuidProp = moonModel.properties.find(_.name == "uuid").get
    uuidProp.required should be(true) // uuid is required
  }

  it should "correctly parse oneOf schema in Galaxy" in {
    val specPath = getClass.getResource("/specs/cosmology.yml").getPath
    val openApi = parser.parseFromFile(specPath)

    val galaxySchema = openApi.getComponents.getSchemas.get("Galaxy")
    galaxySchema should not be null

    // Verify it's a oneOf schema
    galaxySchema.getOneOf should not be null
    galaxySchema.getOneOf.size() should be(2)

    // Verify it contains references to ElipticalGalaxy and SpiralGalaxy
    val schemas = galaxySchema.getOneOf.asScala
    schemas.map(_.get$ref()).toSet should contain allOf(
      "#/components/schemas/ElipticalGalaxy",
      "#/components/schemas/SpiralGalaxy"
    )

    // Generate the models
    val models = modelGenerator.generateModels(openApi)

    // Verify ElipticalGalaxy model
    val ellipticalModel = models.find(_.name == "ElipticalGalaxy").get
    ellipticalModel.properties.map(_.name) should contain("eccentricity")
    val eccentricityProp = ellipticalModel.properties.find(_.name == "eccentricity").get
    eccentricityProp.typeName should be("BigDecimal")
    eccentricityProp.required should be(true)

    // Verify SpiralGalaxy model
    val spiralModel = models.find(_.name == "SpiralGalaxy").get
    spiralModel.properties.map(_.name) should contain("armCount")
    val armCountProp = spiralModel.properties.find(_.name == "armCount").get
    armCountProp.typeName should be("Int")
    armCountProp.required should be(true)
  }

  it should "correctly parse the Star schema with discriminator" in {
    val specPath = getClass.getResource("/specs/cosmology.yml").getPath
    val openApi = parser.parseFromFile(specPath)

    val starSchema = openApi.getComponents.getSchemas.get("Star")
    starSchema should not be null

    // Verify it's a oneOf with discriminator
    starSchema.getOneOf should not be null
    starSchema.getOneOf.size() should be(3)

    val discriminator = starSchema.getDiscriminator
    discriminator.getPropertyName should be("type")
    val mapping = discriminator.getMapping.asScala
    mapping.keySet should contain allOf("RedGiant", "WhiteDwarf", "Supernova")
    mapping.values.toSet should equal(Set(
      "#/components/schemas/RedGiant",
      "#/components/schemas/WhiteDwarf",
      "#/components/schemas/Supernova"
    ))

    val models = modelGenerator.generateModels(openApi)

    val redModel = models.find(_.name == "RedGiant").get
    redModel.properties.map(_.name) should contain allOf("type", "heliumFusion")
    val heliumProp = redModel.properties.find(_.name == "heliumFusion").get
    heliumProp.typeName should be("String")
    heliumProp.required should be(true)

    val whiteModel = models.find(_.name == "WhiteDwarf").get
    whiteModel.properties.map(_.name) should contain allOf("type", "carbonInfusion")
    val carbonProp = whiteModel.properties.find(_.name == "carbonInfusion").get
    carbonProp.typeName should be("String")
    carbonProp.required should be(true)

    val superModel = models.find(_.name == "Supernova").get
    superModel.properties.map(_.name) should contain allOf("type", "phase")
    val phaseProp = superModel.properties.find(_.name == "phase").get
    phaseProp.typeName should be("String")
    phaseProp.required should be(true)
  }
}
