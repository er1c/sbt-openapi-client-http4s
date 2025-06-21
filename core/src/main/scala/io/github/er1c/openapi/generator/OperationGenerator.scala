package io.github.er1c.openapi.generator

import io.github.er1c.openapi.parser.model.OperationParser
import io.github.er1c.openapi.parser.model.ApiOperation
import io.swagger.v3.oas.models.OpenAPI

/**
 * OperationGenerator is a thin wrapper around OperationParser for extracting API operations from OpenAPI specs.
 * @param basePackage The base Scala package for generated code
 */
class OperationGenerator(val basePackage: String) {
  private val parser = new OperationParser(basePackage)

  /**
   * Generate API operations from an OpenAPI specification
   * @param openApi The parsed OpenAPI specification
   * @return A sequence of ApiOperation objects
   */
  def generateOperations(openApi: OpenAPI): Seq[ApiOperation] =
    parser.generateOperations(openApi)
}

