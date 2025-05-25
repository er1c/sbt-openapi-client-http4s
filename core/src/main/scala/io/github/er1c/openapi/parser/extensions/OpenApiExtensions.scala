package io.github.er1c.openapi.parser.extensions

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.{Schema => SwaggerSchema}
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Operation
import scala.collection.JavaConverters._

/**
 * Extensions for the OpenAPI model to make it easier to work with in Scala.
 * These extensions add functionality specifically needed for code generation.
 */
object OpenApiExtensions {

  /**
   * Extensions for OpenAPI class
   */
  implicit class OpenApiOps(val openApi: OpenAPI) extends AnyVal {

    /**
     * Get all operations in the OpenAPI specification
     * @return A sequence of operation IDs mapped to their operations
     */
    def allOperations: Seq[(String, Operation)] = {
      val paths = Option(openApi.getPaths).map(_.asScala).getOrElse(Map.empty)

      paths.flatMap { case (pathName, pathItem) =>
        Seq(
          Option(pathItem.getGet).map(op => (pathName, "get", op)),
          Option(pathItem.getPost).map(op => (pathName, "post", op)),
          Option(pathItem.getPut).map(op => (pathName, "put", op)),
          Option(pathItem.getDelete).map(op => (pathName, "delete", op)),
          Option(pathItem.getPatch).map(op => (pathName, "patch", op)),
          Option(pathItem.getHead).map(op => (pathName, "head", op)),
          Option(pathItem.getOptions).map(op => (pathName, "options", op)),
          Option(pathItem.getTrace).map(op => (pathName, "trace", op))
        ).flatten
      }.map { case (pathName, method, operation) =>
        // Use operationId if available, otherwise generate one from path and method
        val opId = Option(operation.getOperationId).getOrElse(s"${method}_${pathName.replace("/", "_")}")
        (opId, operation)
      }.toSeq
    }

    /**
     * Get all schema definitions in the OpenAPI specification
     * @return A map of schema names to schemas
     */
    def allSchemas: Map[String, SwaggerSchema[_]] = {
      Option(openApi.getComponents)
        .flatMap(c => Option(c.getSchemas))
        .map(_.asScala.toMap)
        .getOrElse(Map.empty)
    }

    /**
     * Get all servers (base URLs) defined in the OpenAPI specification
     * @return A sequence of server URLs
     */
    def serverUrls: Seq[String] = {
      Option(openApi.getServers)
        .map(_.asScala.map(_.getUrl))
        .getOrElse(Seq.empty)
    }
  }

  /**
   * Extensions for PathItem class
   */
  implicit class PathItemOps(val pathItem: PathItem) extends AnyVal {

    /**
     * Get all operations for this path item
     * @return A sequence of HTTP methods and their operations
     */
    def operations: Seq[(String, Operation)] = {
      Seq(
        Option(pathItem.getGet).map(op => ("get", op)),
        Option(pathItem.getPost).map(op => ("post", op)),
        Option(pathItem.getPut).map(op => ("put", op)),
        Option(pathItem.getDelete).map(op => ("delete", op)),
        Option(pathItem.getPatch).map(op => ("patch", op)),
        Option(pathItem.getHead).map(op => ("head", op)),
        Option(pathItem.getOptions).map(op => ("options", op)),
        Option(pathItem.getTrace).map(op => ("trace", op))
      ).flatten
    }
  }

  /**
   * Extensions for Operation class
   */
  implicit class OperationOps(val operation: Operation) extends AnyVal {

    /**
     * Get all parameters for this operation
     * @return A sequence of parameters
     */
    def allParameters: Seq[Parameter] = {
      val opParams = Option(operation.getParameters).map(_.asScala).getOrElse(Seq.empty)
      opParams
    }

    /**
     * Check if this operation has a request body
     * @return True if the operation has a request body
     */
    def hasRequestBody: Boolean = operation.getRequestBody != null

    /**
     * Get all response schema names for this operation
     * @return A map of status codes to schema names
     */
    def responseSchemas: Map[String, String] = {
      Option(operation.getResponses)
        .map(_.asScala.flatMap { case (statusCode, response) =>
          Option(response.getContent).map(_.asScala).getOrElse(Map.empty).flatMap { case (_, mediaType) =>
            Option(mediaType.getSchema).flatMap { schema =>
              // Get schema name from $ref or create a synthetic one based on operationId and status code
              val schemaName = getSchemaNameFromRef(schema) getOrElse {
                val opId = Option(operation.getOperationId).getOrElse("unknown")
                s"${opId}_${statusCode}_response"
              }
              Some((statusCode, schemaName))
            }
          }.headOption
        }.toMap)
        .getOrElse(Map.empty)
    }
  }

  /**
   * Extensions for Schema class
   */
  implicit class SchemaOps(val schema: SwaggerSchema[_]) extends AnyVal {

    /**
     * Get the Scala type for this schema
     * @return The Scala type as a string
     */
    def scalaType: String = {
      val schemaType = Option(schema.getType).getOrElse("")
      val schemaFormat = Option(schema.getFormat).getOrElse("")

      schemaType match {
        case "integer" =>
          schemaFormat match {
            case "int32" => "Int"
            case "int64" => "Long"
            case _ => "Int"
          }
        case "number" =>
          // Use BigDecimal for all number formats for better precision
          "BigDecimal"
        case "string" =>
          schemaFormat match {
            case "byte" => "Array[Byte]"
            case "binary" => "Array[Byte]"
            case "date" => "java.time.LocalDate"
            case "date-time" => "java.time.Instant"
            case "uuid" => "java.util.UUID"
            case _ => "String"
          }
        case "boolean" => "Boolean"
        case "array" =>
          Option(schema.getItems) match {
            case Some(items) =>
              // Get item type - check reference first, then type
              val itemTypeName =
                if (items.get$ref() != null) {
                  // Direct reference to a model
                  getSchemaNameFromRef(items).getOrElse("Any")
                } else if (items.getType != null) {
                  // Primitive type
                  new SchemaOps(items).scalaType
                } else {
                  // Default
                  "Any"
                }
              s"List[$itemTypeName]"
            case None =>
              "List[Any]"
          }
        case "object" =>
          // Check for reference first
          if (schema.get$ref() != null) {
            getSchemaNameFromRef(schema).getOrElse("Any")
          } else {
            Option(schema.getAdditionalProperties) match {
              case Some(additionalProps) =>
                val valueType = new SchemaOps(additionalProps.asInstanceOf[SwaggerSchema[_]]).scalaType
                s"Map[String, $valueType]"
              case None => "Map[String, Any]"
            }
          }
        case _ => getSchemaNameFromRef(schema).getOrElse("Any")
      }
    }
  }

  /**
   * Helper method to extract schema name from a $ref
   * @param schema The schema to extract the name from
   * @return The schema name, or None if it's not a reference
   */
  private def getSchemaNameFromRef(schema: SwaggerSchema[_]): Option[String] = {
    Option(schema.get$ref()).map { ref =>
      // Extract the last part of the reference
      // e.g. #/components/schemas/Pet -> Pet
      ref.split("/").last
    }
  }
}

