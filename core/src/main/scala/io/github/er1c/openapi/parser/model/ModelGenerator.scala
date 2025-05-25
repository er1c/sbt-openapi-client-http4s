package io.github.er1c.openapi.parser.model

import io.github.er1c.openapi.parser.extensions.OpenApiExtensions._
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.{Schema => SwaggerSchema}
import scala.collection.JavaConverters._

/**
 * Represents a property of a schema model
 *
 * @param name The name of the property
 * @param typeName The Scala type of the property
 * @param required Whether the property is required
 * @param description Optional description of the property
 */
case class Property(
  name: String,
  typeName: String,
  required: Boolean,
  description: Option[String] = None
)

/**
 * A model that represents Scala type information generated from OpenAPI schemas.
 * This will be used in the code generation step.
 */
case class ScalaType(
  name: String,
  packageName: String,
  imports: Set[String] = Set.empty,
  properties: Seq[Property] = Seq.empty,
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

    // Store all schemas for reference resolution
    val allSchemas = schemas

    // Generate types with access to all schemas for reference resolution
    schemas.map { case (name, schema) =>
      generateType(name, schema, allSchemas)
    }.toSeq
  }

  /**
   * Generate a Scala type from an OpenAPI schema.
   *
   * @param name The name of the schema
   * @param schema The schema
   * @param allSchemas All schemas in the specification (for reference resolution)
   * @return A ScalaType model
   */
  private def generateType(
    name: String,
    schema: SwaggerSchema[_],
    allSchemas: Map[String, SwaggerSchema[_]]
  ): ScalaType = {
    // First handle allOf schemas (inheritance)
    val allOfProperties = getAllOfProperties(schema, allSchemas)

    // Get properties directly defined in this schema
    val directProperties = Option(schema.getProperties)
      .map(_.asScala.map { case (propName, propSchema) =>
        val required = Option(schema.getRequired)
          .map(_.asScala.contains(propName))
          .getOrElse(false)

        val propType = propSchema.scalaType
        val description = Option(propSchema.getDescription)

        Property(propName, propType, required, description)
      }.toSeq)
      .getOrElse(Seq.empty)

    // Combine properties with inheritance (direct properties take precedence)
    val propertiesMap = scala.collection.mutable.LinkedHashMap[String, Property]()

    // First add inherited properties (they have lower precedence)
    allOfProperties.foreach { prop =>
      propertiesMap.put(prop.name, prop)
    }

    // Then add direct properties (they override inherited ones)
    directProperties.foreach { prop =>
      propertiesMap.put(prop.name, prop)
    }

    // Get the final properties list
    val properties = propertiesMap.values.toSeq

    val imports = gatherImports(properties.map(_.typeName))

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
   * Get properties from allOf schemas (inheritance)
   *
   * @param schema The schema that may contain allOf
   * @param allSchemas All schemas in the specification (for reference resolution)
   * @return A sequence of properties from all parent schemas
   */
  private def getAllOfProperties(
    schema: SwaggerSchema[_],
    allSchemas: Map[String, SwaggerSchema[_]]
  ): Seq[Property] = {
    Option(schema.getAllOf)
      .map { allOfSchemas =>
        // Use flatMap directly to get a flattened sequence
        allOfSchemas.asScala.flatMap { parentSchema =>
          if (parentSchema.get$ref() != null) {
            // For $ref in allOf, resolve the referenced schema
            val refSchemaName = parentSchema.get$ref().split("/").last

            // Get the referenced schema and extract properties
            allSchemas.get(refSchemaName).toSeq.flatMap { refSchema =>
              Option(refSchema.getProperties)
                .map(_.asScala.map { case (propName, propSchema) =>
                  val required = Option(refSchema.getRequired)
                    .map(_.asScala.contains(propName))
                    .getOrElse(false)

                  val propType = propSchema.scalaType
                  val description = Option(propSchema.getDescription)

                  Property(propName, propType, required, description)
                }.toSeq)
                .getOrElse(Seq.empty)
            }
          } else {
            // For inline schemas in allOf, extract their properties
            Option(parentSchema.getProperties)
              .map(_.asScala.map { case (propName, propSchema) =>
                val required = Option(parentSchema.getRequired)
                  .map(_.asScala.contains(propName))
                  .getOrElse(false)

                val propType = propSchema.scalaType
                val description = Option(propSchema.getDescription)

                Property(propName, propType, required, description)
              }.toSeq)
              .getOrElse(Seq.empty)
          }
        }.toSeq
      }
      .getOrElse(Seq.empty)
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
