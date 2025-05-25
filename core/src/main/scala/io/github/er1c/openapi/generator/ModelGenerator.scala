package io.github.er1c.openapi.generator

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media._

import java.io.File
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

class ModelGenerator {

  private val defaultModelsPackage = "io.github.er1c.generated.models" // Fallback

  def generateModels(openApi: OpenAPI, basePackageName: String, outputDir: File): Unit = {
    val modelsPackage = if (basePackageName.isEmpty || basePackageName == ".") defaultModelsPackage else s"$basePackageName.models"
    val components = Option(openApi.getComponents)
    val schemas = components.flatMap(c => Option(c.getSchemas)).map(_.asScala).getOrElse(Map.empty[String, Schema[_]]).toMap

    if (schemas.isEmpty) {
      println("No schemas found in OpenAPI components to generate models from.")
      return
    }

    schemas.foreach { case (originalSchemaName, schema) =>
      val scalaTypeName = ScalaNames.toTypeName(originalSchemaName)
      val resolvedSchema = resolveSchema(schema, openApi) // Resolve if schema itself is a $ref (unlikely for top-level components)

      val fileContent = generateSingleModelFileContent(
        scalaTypeName,
        resolvedSchema,
        modelsPackage,
        openApi,
        schemas
      )
      if (fileContent.nonEmpty) {
        FileUtils.writeFile(outputDir, modelsPackage, scalaTypeName, fileContent)
      }
    }
  }

  private def resolveSchema(s: Schema[_], openApi: OpenAPI): Schema[_] = {
    s match {
      case refSchema: Schema[_] if refSchema.get$ref != null =>
        val refName = refSchema.get$ref.substring(refSchema.get$ref.lastIndexOf('/') + 1)
        Option(openApi.getComponents)
          .flatMap(c => Option(c.getSchemas).map(_.asScala.toMap)) // Convert to Scala Map
          .flatMap(sMap => sMap.get(refName)) // Use get on Scala Map which returns Option
          .map(resolved => resolveSchema(resolved, openApi)) // Recursively resolve
          .getOrElse {
            println(s"Warning: Could not resolve schema reference: ${refSchema.get$ref}")
            s
          }
      case _ => s
    }
  }

  private def generateSingleModelFileContent(
    scalaTypeName: String,
    schema: Schema[_],
    modelsPackage: String,
    openApi: OpenAPI,
    allSchemas: Map[String, Schema[_]]
  ): String = {
    val imports = ListBuffer("import io.circe.{Decoder, Encoder}")
    val description = Option(schema.getDescription).filter(_.nonEmpty).map(d => s"/**\n * ${d.replace("\n", "\n * ").replace("*/", "* /")}\n */\n").getOrElse("")

    val definition: String = schema match {
      case s if s.getEnum != null && !s.getEnum.isEmpty =>
        generateEnum(scalaTypeName, s, imports)
      case s if s.getDiscriminator != null || (s.getOneOf != null && !s.getOneOf.isEmpty && s.getOneOf.asScala.exists(oneOfSchema => oneOfSchema.get$ref != null)) =>
        if (s.getDiscriminator != null) {
          generateSealedTrait(scalaTypeName, s, imports)
        } else {
          println(s"Warning: Schema $scalaTypeName has oneOf but no discriminator. Generating as a simple case class or placeholder.")
          // Potentially generate a coproduct using Circe's auto coproduct derivation if all oneOf are simple types or refs
          // For now, a placeholder or attempt a simple case class if it has properties.
          if (s.getProperties != null && !s.getProperties.isEmpty) {
            generateCaseClass(scalaTypeName, s.asInstanceOf[ObjectSchema], modelsPackage, openApi, allSchemas, imports)
          } else {
            s"// TODO: Implement oneOf for $scalaTypeName (no discriminator, no properties)\ncase class $scalaTypeName() // Placeholder"
          }
        }
      case s: ObjectSchema =>
        if ((s.getProperties != null && !s.getProperties.isEmpty) || (s.getAllOf != null && !s.getAllOf.isEmpty)) {
          generateCaseClass(scalaTypeName, s, modelsPackage, openApi, allSchemas, imports)
        } else if (s.getAdditionalProperties != null) {
          generateMapOpaqueType(scalaTypeName, s, modelsPackage, openApi, allSchemas, imports)
        } else {
          imports += "import io.circe.generic.semiauto._"
          s"case class $scalaTypeName()\nobject $scalaTypeName {\n  implicit val enc: Encoder[$scalaTypeName] = deriveEncoder\n  implicit val dec: Decoder[$scalaTypeName] = deriveDecoder\n}"
        }
      case s: ArraySchema =>
        generateArrayOpaqueType(scalaTypeName, s, modelsPackage, openApi, allSchemas, imports)
      case _: StringSchema => generatePrimitiveOpaqueType(scalaTypeName, "String", imports)
      case s: IntegerSchema =>
        val (underlying, _) = Option(s.getFormat) match { // _fmt changed to _
          case Some("int64") => ("Long", Some("int64"))
          case _             => ("Int", Option(s.getFormat))
        }
        generatePrimitiveOpaqueType(scalaTypeName, underlying, imports)
      case s: NumberSchema =>
        val (underlying, _) = Option(s.getFormat) match { // _fmt changed to _
          case Some("float")  => ("Float", Some("float"))
          case Some("double") => ("Double", Some("double"))
          case _              => ("BigDecimal", Option(s.getFormat))
        }
        generatePrimitiveOpaqueType(scalaTypeName, underlying, imports)
      case _: BooleanSchema => generatePrimitiveOpaqueType(scalaTypeName, "Boolean", imports)
      case _: DateSchema => generatePrimitiveOpaqueType(scalaTypeName, "java.time.LocalDate", imports)
      case _: DateTimeSchema => generatePrimitiveOpaqueType(scalaTypeName, "java.time.OffsetDateTime", imports)
      case _: PasswordSchema => generatePrimitiveOpaqueType(scalaTypeName, "String", imports)
      case _: EmailSchema => generatePrimitiveOpaqueType(scalaTypeName, "String", imports)
      case _: UUIDSchema => generatePrimitiveOpaqueType(scalaTypeName, "java.util.UUID", imports)
      case _: BinarySchema => generatePrimitiveOpaqueType(scalaTypeName, "Array[Byte]", imports) // Or String for Base64
      case s =>
        println(s"Warning: Unhandled schema type for $scalaTypeName: ${s.getClass.getSimpleName}. Type: ${s.getType}, Format: ${s.getFormat}")
        s"// Unhandled schema: $scalaTypeName - Type: ${s.getType}, Format: ${s.getFormat}\ncase class $scalaTypeName() // Placeholder for ${s.getClass.getSimpleName}"
    }

    if (definition.isEmpty || definition.trim.startsWith("// TODO") || definition.trim.startsWith("// Unhandled")) {
      "" // Don't generate file for TODOs or unhandled
    } else {
      s"package $modelsPackage\n\n${imports.distinct.sorted.mkString("\n")}\n\n$description$definition"
    }
  }

  private def generatePrimitiveOpaqueType(
    typeName: String,
    underlyingScalaType: String,
    imports: ListBuffer[String]
  ): String = {
    val (encoderLine, decoderLine) = underlyingScalaType match {
      case "String"     => (s"Encoder.encodeString.contramap(_.value)", s"Decoder.decodeString.map($typeName.apply)")
      case "Int"        => (s"Encoder.encodeInt.contramap(_.value)", s"Decoder.decodeInt.map($typeName.apply)")
      case "Long"       => (s"Encoder.encodeLong.contramap(_.value)", s"Decoder.decodeLong.map($typeName.apply)")
      case "Boolean"    => (s"Encoder.encodeBoolean.contramap(_.value)", s"Decoder.decodeBoolean.map($typeName.apply)")
      case "Float"      => (s"Encoder.encodeFloat.contramap(_.value)", s"Decoder.decodeFloat.map($typeName.apply)")
      case "Double"     => (s"Encoder.encodeDouble.contramap(_.value)", s"Decoder.decodeDouble.map($typeName.apply)")
      case "BigDecimal" =>
        imports += "import scala.math.BigDecimal"
        (s"Encoder.encodeBigDecimal.contramap(_.value)", s"Decoder.decodeBigDecimal.map($typeName.apply)")
      case "java.time.LocalDate" =>
        imports += "import java.time.LocalDate"
        imports += "import java.time.format.DateTimeFormatter"
        imports += "import scala.util.Try"
        (s"Encoder.encodeString.contramap(_.format(DateTimeFormatter.ISO_LOCAL_DATE))",
         s"Decoder.decodeString.emapTry(str => Try(LocalDate.parse(str, DateTimeFormatter.ISO_LOCAL_DATE)).map($typeName.apply).toEither.left.map(_.getMessage))")
      case "java.time.OffsetDateTime" =>
        imports += "import java.time.OffsetDateTime"
        imports += "import java.time.format.DateTimeFormatter"
        imports += "import scala.util.Try"
        (s"Encoder.encodeString.contramap(_.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))",
         s"Decoder.decodeString.emapTry(str => Try(OffsetDateTime.parse(str, DateTimeFormatter.ISO_OFFSET_DATE_TIME)).map($typeName.apply).toEither.left.map(_.getMessage))")
      case "java.util.UUID" =>
        imports += "import java.util.UUID"
        imports += "import scala.util.Try"
        (s"Encoder.encodeString.contramap(_.toString)",
         s"Decoder.decodeString.emapTry(str => Try(UUID.fromString(str)).map($typeName.apply).toEither.left.map(_.getMessage))")
      case "Array[Byte]" => // For BinarySchema, often Base64 encoded string in JSON
        imports += "import java.util.Base64"
        (s"Encoder.encodeString.contramap(bytes => Base64.getEncoder.encodeToString(bytes))",
         s"Decoder.decodeString.map(str => $typeName.apply(Base64.getDecoder.decode(str)))")
      case _ =>
        println(s"Warning: No specific Circe encoder/decoder for opaque type $typeName with underlying $underlyingScalaType.")
        (s"Encoder.encodeString.contramap(_.toString)", s"Decoder.decodeString.map(s => $typeName.apply(s.asInstanceOf[$underlyingScalaType])) // FIXME: May not work")
    }

    s"""opaque type $typeName = $underlyingScalaType

object $typeName {
  def apply(value: $underlyingScalaType): $typeName = value

  extension (t: $typeName)
    def value: $underlyingScalaType = t

  given Encoder[$typeName] = $encoderLine
  given Decoder[$typeName] = $decoderLine
}"""
  }

  private def generateEnum(
    typeName: String,
    schema: Schema[_],
    imports: ListBuffer[String]
  ): String = {
    val enumValues = schema.getEnum.asScala.map(_.toString)
    imports += "import io.circe.Json"

    val cases = enumValues.map(v => s"  case ${ScalaNames.toTypeName(ScalaNames.sanitize(v))}").mkString("\n")
    val encoderCases = enumValues.map(v => s"""case ${ScalaNames.toTypeName(ScalaNames.sanitize(v))} => Json.fromString("${v}")""").mkString("\n    ")
    val decoderCases = enumValues.map(v => s"""case "$v" => Right(${ScalaNames.toTypeName(ScalaNames.sanitize(v))})""").mkString("\n      ")

    s"""enum $typeName {
$cases
}
object $typeName {
  implicit val encoder: Encoder[$typeName] = Encoder.instance {
    $encoderCases
  }
  implicit val decoder: Decoder[$typeName] = Decoder.decodeString.emap {
    str => str match {
      $decoderCases
      case other => Left(s"Unexpected value for enum $typeName: $${other}")
    }
  }
}"""
  }

  private def generateCaseClass(
    typeName: String,
    schema: ObjectSchema,
    modelsPackage: String,
    openApi: OpenAPI,
    allSchemas: Map[String, Schema[_]],
    imports: ListBuffer[String]
  ): String = {
    imports += "import io.circe.generic.semiauto._"

    val localPropertiesMap = Option(schema.getProperties).map(_.asScala.toMap).getOrElse(Map.empty[String, Schema[_]])
    val requiredFields = Option(schema.getRequired).map(_.asScala.toSet).getOrElse(Set.empty[String])

    val localPropertyDefs = localPropertiesMap.map { case (propName, propSchema) =>
      val isNullable = Option(propSchema.getNullable).exists(identity) || !requiredFields.contains(propName)
      // Corrected order: toFieldName then sanitize
      ScalaNames.sanitize(ScalaNames.toFieldName(propName)) -> (propSchema, isNullable)
    }

    val allOfSchemas = Option(schema.getAllOf).map(_.asScala.toList).getOrElse(List.empty)
    val allOfPropertiesMap = allOfSchemas.flatMap { refSchema =>
      resolveSchema(refSchema, openApi) match {
        case objSchema: ObjectSchema =>
          val inheritedRequired = Option(objSchema.getRequired).map(_.asScala.toSet).getOrElse(Set.empty[String])
          Option(objSchema.getProperties).map(_.asScala.toMap).getOrElse(Map.empty).map {
            case (propName, propSch) =>
              val isNullable = Option(propSch.getNullable).exists(identity) || !inheritedRequired.contains(propName)
              // Corrected order: toFieldName then sanitize
              ScalaNames.sanitize(ScalaNames.toFieldName(propName)) -> (propSch, isNullable)
          }
        case _ =>
          println(s"Warning: allOf for $typeName contained a non-object schema: ${Option(refSchema.get$ref).getOrElse("Inline schema")}")
          Map.empty[String, (Schema[_], Boolean)]
      }
    }.toMap // Merges, last one wins for same key from different allOf parts

    val combinedProperties = allOfPropertiesMap ++ localPropertyDefs // Local properties override allOf

    // Generate opaque type wrappers for primitive fields
    val fieldOpaqueTypes = ListBuffer[String]()
    val fieldsWithTypes = combinedProperties.map { case (fieldName, (propSchema, isNullable)) =>
      // Determine if the field should have an opaque type wrapper
      // Always generate opaque types for primitive fields, regardless of nullability
      val shouldGenerateOpaqueType = isPrimitive(propSchema)
      val fieldType = if (shouldGenerateOpaqueType) {
        // Get the underlying type (without Option wrapper)
        val underlyingType = getScalaType(propSchema, modelsPackage, openApi, allSchemas, isPropertyNullable = false, imports)

        // Create opaque type name: TypeName + FieldName
        val opaqueTypeName = s"${typeName}${ScalaNames.toTypeName(fieldName)}"

        // Generate the opaque type definition
        fieldOpaqueTypes += generatePrimitiveOpaqueType(opaqueTypeName, underlyingType, imports)

        // Use the opaque type in the field
        if (isNullable) s"Option[$opaqueTypeName]" else opaqueTypeName
      } else {
        // Use standard type for complex types
        getScalaType(propSchema, modelsPackage, openApi, allSchemas, isPropertyNullable = isNullable, imports)
      }

      val propDescription = Option(propSchema.getDescription).filter(_.nonEmpty)
        .map(d => s"/** ${d.trim.replace("\n", " ").replace("*/", "* /")} */\n  ")
        .getOrElse("  ")

      (fieldName, fieldType, propDescription)
    }

    val fieldsStr = fieldsWithTypes.map { case (fieldName, fieldType, propDescription) =>
      s"$propDescription$fieldName: $fieldType"
    }.mkString(",\n")

    if (combinedProperties.isEmpty) {
      s"case class $typeName()\nobject $typeName {\n  implicit val encoder: Encoder[$typeName] = deriveEncoder[$typeName]\n  implicit val decoder: Decoder[$typeName] = deriveDecoder[$typeName]\n}"
    } else {
      val opaqueTypesStr = if (fieldOpaqueTypes.nonEmpty) fieldOpaqueTypes.mkString("\n\n") + "\n\n" else ""
      s"""$opaqueTypesStr
case class $typeName(\n${fieldsStr.split("\n").map(l => if(l.trim.startsWith("/**")) l else "  " + l).mkString("\n")}\n)\nobject $typeName {\n  implicit val encoder: Encoder[$typeName] = deriveEncoder[$typeName]\n  implicit val decoder: Decoder[$typeName] = deriveDecoder[$typeName]\n}"""
    }
  }

  // Helper method to determine if a schema is for a primitive type
  private def isPrimitive(schema: Schema[_]): Boolean = {
    schema match {
      case _: StringSchema => true
      case _: IntegerSchema => true
      case _: NumberSchema => true
      case _: BooleanSchema => true
      case _: DateSchema => true
      case _: DateTimeSchema => true
      case _: UUIDSchema => true
      case _ => false
    }
  }

  private def generateSealedTrait(
    traitName: String,
    schema: Schema[_],
    imports: ListBuffer[String]
  ): String = {
    imports += "import io.circe.syntax._"
    // Concrete types might use generic.semiauto, but the trait itself doesn't directly.

    val disc = schema.getDiscriminator
    if (disc == null) {
      return s"// Sealed trait $traitName generation skipped: No discriminator found."
    }
    val discriminatorPropertyJsonName = disc.getPropertyName // Use original JSON name for lookup
    val discriminatorPropertyScalaName = ScalaNames.toFieldName(ScalaNames.sanitize(discriminatorPropertyJsonName))

    val concreteTypeInfos = Option(disc.getMapping).map(_.asScala.map { case (discValue, ref) =>
      (discValue, ScalaNames.toTypeName(ref.substring(ref.lastIndexOf('/') + 1)))
    }.toList).getOrElse {
      Option(schema.getOneOf).map(_.asScala.toList.collect { case refSchema if refSchema.get$ref != null =>
        val typeName = ScalaNames.toTypeName(refSchema.get$ref.substring(refSchema.get$ref.lastIndexOf('/') + 1))
        val discValue = ScalaNames.toFieldName(typeName) // Infer: e.g., Cat -> cat. Or use original ref name.
        (discValue, typeName)
      }).getOrElse(List.empty)
    }

    if (concreteTypeInfos.isEmpty) {
      return s"// Sealed trait $traitName generation skipped: No concrete types found from discriminator mapping or oneOf refs."
    }

    val decoderCases = concreteTypeInfos.map { case (discValue, concreteTypeName) =>
      s"""case "$discValue" => c.as[$concreteTypeName]"""
    }.mkString("\n      ")

    val encoderCases = concreteTypeInfos.map { case (_, concreteTypeName) =>
      s"case t: $concreteTypeName => t.asJson"
    }.mkString("\n      ")

    s"""sealed trait $traitName {
  def $discriminatorPropertyScalaName: String // The discriminator property
}

object $traitName {
  implicit val decoder: Decoder[$traitName] = Decoder.instance { c =>
    c.downField("${discriminatorPropertyJsonName}").as[String].flatMap {
      $decoderCases
      case other => Left(io.circe.DecodingFailure(s"Unknown value '$${other}' for discriminator '${discriminatorPropertyJsonName}' in $traitName", c.history))
    }
  }

  implicit val encoder: Encoder[$traitName] = Encoder.instance {
    $encoderCases
  }
}"""
  }

  private def generateArrayOpaqueType(
    typeName: String,
    schema: ArraySchema,
    modelsPackage: String,
    openApi: OpenAPI,
    allSchemas: Map[String, Schema[_]],
    imports: ListBuffer[String]
  ): String = {
    val itemSchema = Option(schema.getItems).getOrElse(new Schema().description("Generic array item type, defaulting to String"))
    val itemTypeName = getScalaType(itemSchema, modelsPackage, openApi, allSchemas, isPropertyNullable = false, imports)

    s"""opaque type $typeName = List[$itemTypeName]

object $typeName {
  def apply(value: List[$itemTypeName]): $typeName = value

  extension (t: $typeName)
    def value: List[$itemTypeName] = t

  given (using itemEncoder: Encoder[$itemTypeName]): Encoder[$typeName] = Encoder.encodeList[$itemTypeName].contramap(_.value)
  given (using itemDecoder: Decoder[$itemTypeName]): Decoder[$typeName] = Decoder.decodeList[$itemTypeName].map($typeName.apply)
}"""
  }

 private def generateMapOpaqueType(
    typeName: String,
    schema: ObjectSchema, // This is the schema for the map itself
    modelsPackage: String,
    openApi: OpenAPI,
    allSchemas: Map[String, Schema[_]],
    imports: ListBuffer[String]
  ): String = {
    val valueSchema = schema.getAdditionalProperties.asInstanceOf[Schema[_]]
    val valueTypeName = getScalaType(valueSchema, modelsPackage, openApi, allSchemas, isPropertyNullable = false, imports)

    s"""opaque type $typeName = Map[String, $valueTypeName]

object $typeName {
  def apply(value: Map[String, $valueTypeName]): $typeName = value

  extension (t: $typeName)
    def value: Map[String, $valueTypeName] = t

  given (using valueEncoder: Encoder[$valueTypeName]): Encoder[$typeName] = Encoder.encodeMap[String, $valueTypeName].contramap(_.value)
  given (using valueDecoder: Decoder[$valueTypeName]): Decoder[$typeName] = Decoder.decodeMap[String, $valueTypeName].map($typeName.apply)
}"""
  }

  def getScalaType(
    propSchema: Schema[_],
    currentModelsPackage: String, // Not actively used yet, but could be for FQNs
    openApi: OpenAPI,
    allSchemas: Map[String, Schema[_]],
    isPropertyNullable: Boolean,
    imports: ListBuffer[String] // Added imports parameter
  ): String = {
    val actualSchema = resolveSchema(propSchema, openApi)

    val coreType: String = actualSchema match {
      case s if s.get$ref != null =>
        ScalaNames.toTypeName(s.get$ref.substring(s.get$ref.lastIndexOf('/') + 1))
      case s: ArraySchema =>
        val itemSch = Option(s.getItems).getOrElse(new StringSchema().description("Defaulting to String for underspecified array items"))
        s"List[${getScalaType(itemSch, currentModelsPackage, openApi, allSchemas, isPropertyNullable = false, imports)}]"
      case s: ObjectSchema if s.getAdditionalProperties != null && (s.getProperties == null || s.getProperties.isEmpty) =>
        // Inline map definition for a property
        val valSch = s.getAdditionalProperties.asInstanceOf[Schema[_]]
        s"Map[String, ${getScalaType(valSch, currentModelsPackage, openApi, allSchemas, isPropertyNullable = false, imports)}]"
      case s: ObjectSchema if (s.getProperties == null || s.getProperties.isEmpty) && s.getAdditionalProperties == null && s.getAllOf == null && s.getEnum == null =>
        imports += "import io.circe.JsonObject"
        "JsonObject" // Anonymous empty object
      case s: StringSchema => Option(s.getFormat) match {
        case Some("date") => imports += "import java.time.LocalDate"; "LocalDate"
        case Some("date-time") => imports += "import java.time.OffsetDateTime"; "OffsetDateTime"
        case Some("byte") => "String" // Base64
        case Some("binary") => imports += "import java.util.Base64"; "Array[Byte]" // Raw bytes, may be handled as Base64 string in JSON context by encoder/decoder
        case Some("password") => "String"
        case Some("email") => "String"
        case Some("uuid") => imports += "import java.util.UUID"; "UUID"
        case _ => "String"
      }
      case s: IntegerSchema => Option(s.getFormat) match {
        case Some("int64") => "Long"
        case _ => "Int"
      }
      case s: NumberSchema => Option(s.getFormat) match {
        case Some("float") => "Float"
        case Some("double") => "Double"
        case _ => imports += "import scala.math.BigDecimal"; "BigDecimal"
      }
      case _: BooleanSchema => "Boolean"
      case _: DateSchema => imports += "import java.time.LocalDate"; "LocalDate"
      case _: DateTimeSchema => imports += "import java.time.OffsetDateTime"; "OffsetDateTime"
      case _: PasswordSchema => "String"
      case _: EmailSchema => "String"
      case _: UUIDSchema => imports += "import java.util.UUID"; "UUID"
      case _: BinarySchema => imports += "import java.util.Base64"; "Array[Byte]"
      case s => // Fallback for unrecognized inline schema or complex object schema used as property type
        val schemaNameFromAll = allSchemas.find { case (_, sch) => sch eq s }.map(_._1)
        schemaNameFromAll.map(ScalaNames.toTypeName).getOrElse {
            println(s"Warning: Unrecognized inline schema type for property: ${s.getType}, format: ${s.getFormat}. Defaulting to io.circe.Json.")
            imports += "import io.circe.Json"
            "Json"
        }
    }
    if (isPropertyNullable) s"Option[$coreType]" else coreType
  }
}


