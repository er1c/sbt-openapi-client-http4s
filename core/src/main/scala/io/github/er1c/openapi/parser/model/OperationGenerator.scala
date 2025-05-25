package io.github.er1c.openapi.parser.model

import io.github.er1c.openapi.parser.extensions.OpenApiExtensions._
import io.swagger.v3.oas.models.media.{MediaType, Schema}
import io.swagger.v3.oas.models.{OpenAPI, PathItem}

import scala.collection.JavaConverters._

/**
 * Represents a parameter in an API operation
 */
case class OperationParameter(
  name: String,
  typeName: String,
  required: Boolean,
  description: Option[String],
  in: String // "query", "header", "path", "cookie"
)

/**
 * Represents a request body in an API operation
 */
case class RequestBody(
  typeName: String,
  required: Boolean,
  description: Option[String],
  contentType: String // e.g. "application/json"
)

/**
 * Represents a response in an API operation
 */
case class ResponseInfo(
  statusCode: String,
  typeName: String,
  description: Option[String],
  contentType: String // e.g. "application/json"
)

/**
 * Represents an API operation
 */
case class ApiOperation(
  operationId: String,
  httpMethod: String,
  path: String,
  summary: Option[String],
  description: Option[String],
  parameters: Seq[OperationParameter],
  requestBody: Option[RequestBody],
  responses: Seq[ResponseInfo],
  tags: Seq[String]
)

/**
 * Generator for API operations from OpenAPI spec
 */
class OperationGenerator(val basePackage: String) {

  /**
   * Generate API operations from an OpenAPI specification
   *
   * @param openApi The parsed OpenAPI specification
   * @return A sequence of API operations
   */
  def generateOperations(openApi: OpenAPI): Seq[ApiOperation] = {
    val paths: collection.Map[String, PathItem] =
      Option(openApi.getPaths)
        .map(_.asScala)
        .getOrElse(Map.empty)

    paths.flatMap { case (path, pathItem) =>
      pathItem.operations.map { case (method, operation) =>
        val operationId =
          Option(operation.getOperationId)
            .getOrElse(s"${method}_${path.replace("/", "_")}")

        // Extract parameters
        val parameters = operation.allParameters.map { param =>
          val schema = Option(param.getSchema)
          val typeName = schema.map(_.scalaType).getOrElse("String")

          OperationParameter(
            name = param.getName,
            typeName = typeName,
            required = Option(param.getRequired).exists(_.booleanValue),
            description = Option(param.getDescription),
            in = param.getIn
          )
        }

        // Extract request body
        val requestBody = Option(operation.getRequestBody).map { rb =>
          val content: collection.Map[String, MediaType] =
            Option(rb.getContent)
              .map(_.asScala)
              .getOrElse(Map.empty)
          val contentTypeKey: String = content.keys.headOption.getOrElse("application/json")
          val mediaType: Option[MediaType] = content.get(contentTypeKey)

          val schema: Option[Schema[_]] = mediaType.map(mt => Option(mt.getSchema)).flatten
          val typeName = schema.map(_.scalaType).getOrElse("String")

          RequestBody(
            typeName = typeName,
            required = Option(rb.getRequired).exists(_.booleanValue),
            description = Option(rb.getDescription),
            contentType = contentTypeKey
          )
        }

        // Extract responses
        val responses = Option(operation.getResponses).map(_.asScala).getOrElse(Map.empty).flatMap {
          case (statusCode, response) =>
            val content = Option(response.getContent).map(_.asScala).getOrElse(Map.empty)

            if (content.isEmpty) {
              // No content - use Unit for 204/205, Any for others
              val typeName = statusCode match {
                case "204" | "205" => "Unit"
                case _ => "Any"
              }

              Some(ResponseInfo(
                statusCode = statusCode,
                typeName = typeName,
                description = Option(response.getDescription),
                contentType = "application/json" // Default even if no content
              ))
            } else {
              // Get first content type
              val entry = content.head
              val contentTypeKey = entry._1
              val mediaType = entry._2

              val schema = Option(mediaType.getSchema)
              val typeName = schema.flatMap { s =>
                // First check if it's an array with $ref items
                if (s.getType == "array" && s.getItems != null && s.getItems.get$ref() != null) {
                  // Direct handling for array with $ref
                  val refModelName = s.getItems.get$ref().split("/").last
                  Some(s"List[$refModelName]")
                } else if (s.get$ref() != null) {
                  // Direct handling for $ref
                  Some(s.get$ref().split("/").last)
                } else {
                  // Use the scalaType extension method for other cases
                  Some(s.scalaType)
                }
              }.getOrElse("Any")

              Some(ResponseInfo(
                statusCode = statusCode,
                typeName = typeName,
                description = Option(response.getDescription),
                contentType = contentTypeKey
              ))
            }
        }.toSeq

        // Extract tags
        val tags = Option(operation.getTags).map(_.asScala).getOrElse(Seq.empty)

        ApiOperation(
          operationId = operationId,
          httpMethod = method,
          path = path,
          summary = Option(operation.getSummary),
          description = Option(operation.getDescription),
          parameters = parameters,
          requestBody = requestBody,
          responses = responses,
          tags = tags
        )
      }
    }.toSeq
  }
}
