package io.github.er1c.openapi.parser.model

import io.github.er1c.openapi.parser.extensions.OpenApiExtensions._
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.{Schema => SwaggerSchema}
import scala.collection.JavaConverters._

/**
 * A model that represents Scala type information generated from OpenAPI schemas.
 * This will be used in the code generation step.
 */
case class ScalaType(
  name: String,
  packageName: String,
  imports: Set[String] = Set.empty,
  properties: Seq[(String, String, Boolean)] = Seq.empty, // (name, type, required)
  isEnum: Boolean = false,
  enumValues: Seq[String] = Seq.empty,
  description: Option[String] = None
)

/**
 * A model generator that extracts Scala type information from an OpenAPI specification.
 */
class ModelGenerator(val basePackage: String) {

  /**
   * Generate Scala type models from an OpenAPI specification.
   *
   * @param openApi The parsed OpenAPI specification
   * @return A sequence of ScalaType models
   */
  def generateModels(openApi: OpenAPI): Seq[ScalaType] = {
    val schemas = openApi.allSchemas

    schemas.map { case (name, schema) =>
      generateType(name, schema)
    }.toSeq
  }

  /**
   * Generate a Scala type from an OpenAPI schema.
   *
   * @param name The name of the schema
   * @param schema The schema
   * @return A ScalaType model
   */
  private def generateType(name: String, schema: SwaggerSchema[_]): ScalaType = {
    val properties = Option(schema.getProperties)
      .map(_.asScala.map { case (propName, propSchema) =>
        val required = Option(schema.getRequired)
          .map(_.asScala.contains(propName))
          .getOrElse(false)

        val propType = propSchema.scalaType
        (propName, propType, required)
      }.toSeq)
      .getOrElse(Seq.empty)

    val imports = gatherImports(properties.map(_._2))

    // Check if this is an enum
    val isEnum = Option(schema.getEnum).exists(!_.isEmpty)
    val enumValues = if (isEnum) {
      Option(schema.getEnum).map(_.asScala.map(_.toString)).getOrElse(Seq.empty)
    } else {
      Seq.empty
    }

    ScalaType(
      name = name,
      packageName = s"$basePackage.model",
      imports = imports,
      properties = properties,
      isEnum = isEnum,
      enumValues = enumValues,
      description = Option(schema.getDescription)
    )
  }

  /**
   * Gather imports needed for the given types.
   *
   * @param types The types to gather imports for
   * @return A set of import statements
   */
  private def gatherImports(types: Seq[String]): Set[String] = {
    val imports = Set.newBuilder[String]

    types.foreach {
      case t if t.startsWith("java.time.") =>
        imports += t
      case t if t.contains("OffsetDateTime") =>
        imports += "java.time.OffsetDateTime"
      case t if t.contains("LocalDate") =>
        imports += "java.time.LocalDate"
      case t if t.contains("BigDecimal") =>
        imports += "scala.math.BigDecimal"
      case t if t.contains("Map[") =>
        imports += "scala.collection.immutable.Map"
      case t if t.contains("List[") =>
        imports += "scala.collection.immutable.List"
      case _ => // No import needed
    }

    imports.result()
  }
}
