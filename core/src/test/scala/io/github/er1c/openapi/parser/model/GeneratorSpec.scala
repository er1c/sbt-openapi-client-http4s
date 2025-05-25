package io.github.er1c.openapi.parser.model

import io.github.er1c.openapi.parser.OpenApiParser
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
 * Tests for the ModelGenerator and OperationGenerator
 */
class GeneratorSpec extends AnyFlatSpec with Matchers {

  val parser = new OpenApiParser()
  val modelGenerator = new ModelGenerator("io.github.er1c.test")
  val operationGenerator = new OperationGenerator("io.github.er1c.test")

  "ModelGenerator" should "extract correct models from the petstore spec" in {
    val specPath = getClass.getResource("/specs/petstore_3.0.4.yml").getPath
    val openApi = parser.parseFromFile(specPath)

    val models = modelGenerator.generateModels(openApi)

    // Check that key models exist
    models.map(_.name) should contain allOf("Pet", "Category", "Tag", "ApiResponse")

    // Find the Pet model
    val petModel = models.find(_.name == "Pet").get

    // Check some properties of the Pet model
    petModel.properties.map(_.name) should contain allOf("id", "name", "status")

    // Check types
    val idProp = petModel.properties.find(_.name == "id").get
    idProp.typeName should be("Long")

    val nameProp = petModel.properties.find(_.name == "name").get
    nameProp.typeName should be("String")
    nameProp.required should be(true) // required

    // Check imports
    petModel.imports should not be empty
  }

  "OperationGenerator" should "extract correct operations from the petstore spec" in {
    val specPath = getClass.getResource("/specs/petstore_3.0.4.yml").getPath
    val openApi = parser.parseFromFile(specPath)

    val operations = operationGenerator.generateOperations(openApi)

    // Find a specific operation (e.g. addPet)
    val addPet = operations.find(_.operationId == "addPet")
    addPet should not be None

    addPet.foreach { op =>
      op.httpMethod should be("post")
      op.path should be("/pet")
      op.tags should contain("pet")

      // Check request body
      op.requestBody should not be None
      op.requestBody.foreach { rb =>
        rb.typeName should be("Pet")
        rb.required should be(true)
      }

      // Check response
      op.responses should not be empty
      op.responses.map(_.statusCode) should contain allOf("200", "400")
    }

    // Find operation with parameters
    val findPetsByStatus = operations.find(_.operationId == "findPetsByStatus")
    findPetsByStatus should not be None

    findPetsByStatus.foreach { op =>
      op.parameters should not be empty

      // Check the 'status' parameter
      val statusParam = op.parameters.find(_.name == "status")
      statusParam should not be None
      statusParam.foreach { param =>
        param.in should be("query")
        param.required should be(false)
      }

      // Check response type for 200 status
      val successResponse = op.responses.find(_.statusCode == "200")
      successResponse should not be None
      successResponse.foreach { resp =>
        resp.typeName should be("List[Pet]")
      }
    }
  }
}
