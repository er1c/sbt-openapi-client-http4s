package io.github.er1c.openapi.parser

import io.swagger.parser.OpenAPIParser
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.core.models.ParseOptions
import scala.collection.JavaConverters._

/**
 * OpenAPI specification parser that leverages the swagger-parser library.
 * This class provides functionality to parse and validate OpenAPI/Swagger specifications.
 */
class OpenApiParser {
  /**
   * Parse and validate an OpenAPI specification from a file path.
   *
   * @param filePath Path to the OpenAPI specification file (YAML or JSON)
   * @return Parsed OpenAPI specification
   */
  def parseFromFile(filePath: String): OpenAPI = {
    val parseOptions = new ParseOptions()
    parseOptions.setResolve(true) // Resolve references
    // We don't want to resolve fully as that can replace $ref pointers with actual models
    parseOptions.setResolveFully(false)

    val result = new OpenAPIParser().readLocation(filePath, null, parseOptions)

    if (result.getOpenAPI == null) {
      val errors = Option(result.getMessages)
        .map(messages => messages.asScala.mkString(", "))
        .getOrElse("Unknown error")
      throw new IllegalArgumentException(s"Failed to parse OpenAPI specification: $errors")
    }

    result.getOpenAPI
  }

  /**
   * Parse and validate an OpenAPI specification from a string content.
   *
   * @param content OpenAPI specification content as a string (YAML or JSON)
   * @return Parsed OpenAPI specification
   */
  def parseFromString(content: String): OpenAPI = {
    val parseOptions = new ParseOptions()
    parseOptions.setResolve(true)
    parseOptions.setResolveFully(true)

    val result = new OpenAPIParser().readContents(content, null, parseOptions)

    if (result.getOpenAPI == null) {
      val errors = Option(result.getMessages)
        .map(messages => messages.asScala.mkString(", "))
        .getOrElse("Unknown error")
      throw new IllegalArgumentException(s"Failed to parse OpenAPI specification: $errors")
    }

    result.getOpenAPI
  }
}
