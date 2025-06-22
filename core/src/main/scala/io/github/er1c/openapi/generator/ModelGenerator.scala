package io.github.er1c.openapi.generator

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media._

import java.io.File
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

object ModelGenerator {
  private val defaultModelsPackage = "io.github.er1c.generated.models"

  /**
   * Generate a map of Scala file names to their contents, for all models.
   * The key is the Scala type name (e.g., "Moon"), the value is the file content.
   * This is a pure function and does not write files.
   */
  def generateModels(openApi: OpenAPI, basePackageName: String): Map[String, String] = {
    val modelsPackage = if (basePackageName.isEmpty || basePackageName == ".") defaultModelsPackage else s"$basePackageName.models"
    val components = Option(openApi.getComponents)
    val schemas = components.flatMap(c => Option(c.getSchemas)).map(_.asScala).getOrElse(Map.empty[String, Schema[_]]).toMap

    if (schemas.isEmpty) {
      Map.empty
    } else {
      // Find all oneOf parent schemas that don't have discriminators
      val oneOfParents: Set[String] = schemas.collect {
        case (parentName, parentSchema) if Option(resolveSchema(parentSchema, openApi).getOneOf).exists(_.asScala.exists(_.get$ref != null)) =>
          ScalaNames.toTypeName(parentName)
      }.toSet

      // Track schema names that will be included in ADTs and allOf children that should be skipped
      val schemasIncludedInADTs = scala.collection.mutable.Set[String]()

      // First collect all one-of children to exclude them from standalone generation
      schemas.foreach { case (originalSchemaName, schema) =>
        val resolvedSchema = resolveSchema(schema, openApi)
        // Add oneOf children
        if (resolvedSchema.getOneOf != null && !resolvedSchema.getOneOf.isEmpty) {
          Option(resolvedSchema.getOneOf).foreach(_.asScala.foreach { oneOfSchema =>
            if (oneOfSchema.get$ref != null) {
              val refPath = oneOfSchema.get$ref
              val childName = refPath.substring(refPath.lastIndexOf('/') + 1)
              schemasIncludedInADTs.add(childName)
            }
          })
        }
        // Add allOf children
        if (Option(schema.getAllOf).exists(_.asScala.exists(_.get$ref != null))) {
          schemasIncludedInADTs.add(originalSchemaName)
        }
      }

      // --- Detect allOf inheritance relationships ---
      // Find all schemas that are referenced via allOf (i.e., subclasses)
      val allOfChildren: Map[String, List[String]] = schemas.collect {
        case (childName, schema) if Option(schema.getAllOf).exists(_.asScala.exists(_.get$ref != null)) =>
          val parentNames = schema.getAllOf.asScala.collect {
            case ref if ref.get$ref != null =>
              ref.get$ref.substring(ref.get$ref.lastIndexOf('/') + 1)
          }.toList
          childName -> parentNames
      }
      // Invert: parent -> List[child]
      val allOfParents: Map[String, List[String]] = allOfChildren
        .flatMap { case (child, parents) => parents.map(_ -> child) }
        .groupBy(_._1).mapValues(_.map(_._2).toList)

      // Now generate the model files, skipping children that are already included in ADTs
      schemas.flatMap { case (originalSchemaName, schema) =>
        println(s"[ModelGenerator] Processing schema: $originalSchemaName of type ${schema.getClass.getSimpleName}")
        val scalaTypeName = ScalaNames.toTypeName(originalSchemaName)
        val resolvedSchema = resolveSchema(schema, openApi)
        // --- If this is a parent in allOf, generate a sealed trait and all children as case classes ---
        if (allOfParents.contains(originalSchemaName)) {
          val children = allOfParents(originalSchemaName)
          val childrenDefsAndImports = children.map { childName =>
            val childSchema = schemas(childName)
            generateCaseClassWithOpaqueTypes(ScalaNames.toTypeName(childName), childSchema, modelsPackage, openApi, schemas, Some(scalaTypeName))
          }
          val childrenDefs = childrenDefsAndImports.map(_._1).mkString("\n\n")
          val childrenImports = childrenDefsAndImports.flatMap(_._2)

          // Get properties from the parent schema to add as abstract defs in the trait
          val objectSchema = resolvedSchema.asInstanceOf[ObjectSchema]
          val propertiesMap = Option(objectSchema.getProperties).map(_.asScala.toMap).getOrElse(Map.empty[String, Schema[_]])
          val requiredFields = Option(objectSchema.getRequired).map(_.asScala.toSet).getOrElse(Set.empty[String])
          val propertyDefs = propertiesMap.map { case (propName, propSchema) =>
            val isNullable = Option(propSchema.getNullable).exists(identity) || !requiredFields.contains(propName)
            val propType = if (isPrimitive(propSchema)) {
              val fieldName = ScalaNames.sanitize(ScalaNames.toFieldName(propName))
              val opaqueTypeName = s"${scalaTypeName}${ScalaNames.toTypeName(fieldName)}"
              if (isNullable) s"Option[$opaqueTypeName]" else opaqueTypeName
            } else {
              getScalaType(propSchema, modelsPackage, openApi, schemas, isPropertyNullable = isNullable, ListBuffer())
            }
            s"  def ${ScalaNames.sanitize(ScalaNames.toFieldName(propName))}: $propType"
          }

          val allImports = (
            Seq(
              "import io.circe.*",
              "import io.circe.generic.semiauto.*",
              "import io.circe.syntax.*",
              "import cats.syntax.functor.*", // For .widen
              "import io.circe.generic.auto.*"
            ) ++ childrenImports
          ).distinct.sorted.mkString("\n")

          // Include opaque types for primitive properties in parent trait
          val opaqueTypes = propertiesMap.collect {
            case (propName, propSchema) if isPrimitive(propSchema) =>
              val fieldName = ScalaNames.sanitize(ScalaNames.toFieldName(propName))
              val opaqueTypeName = s"${scalaTypeName}${ScalaNames.toTypeName(fieldName)}"
              val underlyingType = getScalaType(propSchema, modelsPackage, openApi, schemas, isPropertyNullable = false, ListBuffer())
              generatePrimitiveOpaqueType(opaqueTypeName, underlyingType, ListBuffer())
          }

          val opaqueTypesStr = if (opaqueTypes.nonEmpty) opaqueTypes.mkString("\n\n") + "\n\n" else ""
          val propertyDefsStr = if (propertyDefs.nonEmpty) propertyDefs.mkString("\n") + "\n" else ""

          val adtCode = s"""$allImports

sealed trait $scalaTypeName {
$propertyDefsStr}

$opaqueTypesStr
$childrenDefs

object $scalaTypeName:
  given Decoder[$scalaTypeName] = List[Decoder[$scalaTypeName]](
    ${children.map(c => s"summon[Decoder[${ScalaNames.toTypeName(c)}]].map[${scalaTypeName}](identity)").mkString(",\n    ")}
  ).reduceLeft(_ `or` _)

  given Encoder.AsObject[$scalaTypeName] = Encoder.AsObject.instance {
    ${children.map(c => s"case v: ${ScalaNames.toTypeName(c)} => v.asJsonObject").mkString("\n    ")}
  }
"""
          Some(scalaTypeName -> s"package $modelsPackage\n\n$adtCode")
        } else if (schemasIncludedInADTs.contains(originalSchemaName)) {
          None // Skip ADT children
        } else if (resolvedSchema.getDiscriminator != null) {
          // --- Discriminator ADT generation (e.g., Star) ---
          val childRefs = Option(resolvedSchema.getOneOf).map(_.asScala.map(_.get$ref)).getOrElse(Seq.empty)

          val childrenGenData = childRefs.map { ref =>
            val childName = ref.substring(ref.lastIndexOf('/') + 1)
            val childSchema = schemas(childName)
            generateCaseClassWithOpaqueTypes(ScalaNames.toTypeName(childName), childSchema, modelsPackage, openApi, schemas, Some(scalaTypeName))
          }
          val childrenDefs = childrenGenData.map(_._1).mkString("\n\n")
          val childrenImports = childrenGenData.flatMap(_._2)

          val traitImports = ListBuffer[String]()
          val traitAndCompanion = generateSealedTrait(scalaTypeName, resolvedSchema, traitImports, modelsPackage)

          val allImports = (
            Seq(
              "import io.circe.*",
              "import io.circe.generic.semiauto.*",
              "import io.circe.syntax.*",
              "import cats.syntax.functor.*" // For .widen
            ) ++ traitImports ++ childrenImports
          ).distinct.sorted.mkString("\n")

          val disc = resolvedSchema.getDiscriminator
          val discriminatorProperty = ScalaNames.sanitize(ScalaNames.toFieldName(disc.getPropertyName))

          val adtCode = s"""$allImports

sealed trait $scalaTypeName {
  def $discriminatorProperty: String // The discriminator property
}

$childrenDefs

object $scalaTypeName:
  given Decoder[$scalaTypeName] = {
    val decoders = List[Decoder[$scalaTypeName]](
      ${childRefs.map(ref => s"summon[Decoder[${ScalaNames.toTypeName(ref.substring(ref.lastIndexOf('/') + 1))}]].map(identity)").mkString(",\n      ")}
    )
    decoders.reduceLeft(_ `or` _)
  }

  given Encoder.AsObject[$scalaTypeName] = Encoder.AsObject.instance {
    ${childRefs.map(ref => s"case v: ${ScalaNames.toTypeName(ref.substring(ref.lastIndexOf('/') + 1))} => v.asJsonObject").mkString("\n    ")}
  }
"""
          Some(scalaTypeName -> s"package $modelsPackage\n\n$adtCode")
        } else if (oneOfParents.contains(scalaTypeName)) {
          // --- Non-discriminator ADT generation (e.g., Galaxy) ---
          val childRefs = Option(resolvedSchema.getOneOf)
            .map(_.asScala.map { oneOfSchema =>
              if (oneOfSchema.get$ref != null) {
                val refPath = oneOfSchema.get$ref
                val childName = refPath.substring(refPath.lastIndexOf('/') + 1)
                (ScalaNames.toTypeName(childName), childName) // (typeName, refName)
              } else {
                val childName = s"${scalaTypeName}Child$${scala.util.Random.nextInt(1000)}"
                (childName, null)
              }
            }.toSeq).getOrElse(Seq.empty)

          // Get actual child schemas from references
          val childSchemaMap: Map[String, Schema[_]] = childRefs.flatMap { case (typeName, refName) =>
            if (refName == null) None
            else schemas.get(refName).map(schema => typeName -> schema)
          }.toMap

          // Generate case class definitions for children
          val childrenGenData = childSchemaMap.map { case (typeName, schema) =>
            generateCaseClassWithOpaqueTypes(typeName, schema, modelsPackage, openApi, schemas, Some(scalaTypeName))
          }
          val childrenDefs = childrenGenData.map(_._1).mkString("\n\n")
          val childrenImports = childrenGenData.flatMap(_._2)

          val allImports = (
            Seq(
              "import io.circe.*",
              "import io.circe.generic.semiauto.*",
              "import io.circe.syntax.*",
              "import cats.syntax.functor.*",
              "import io.circe.generic.auto.*"
            ) ++ childrenImports
          ).distinct.sorted.mkString("\n")

          val adtCode = s"""$allImports

sealed trait $scalaTypeName

$childrenDefs

object $scalaTypeName {
  given Decoder[$scalaTypeName] = ${childRefs.map(t => s"Decoder[${t._1}].widen").mkString(".or(")}${")" * (childRefs.size - 1)}
  given Encoder[$scalaTypeName] = Encoder.instance {
    ${childRefs.map(t => s"case v: ${t._1} => Encoder[${t._1}].apply(v)").mkString("\n    ")}
  }
}
"""
          Some(scalaTypeName -> s"package $modelsPackage\n\n$adtCode")
        } else {
          // Fallback: treat any schema with allOf as ObjectSchema for code generation
          val hasAllOf = Option(resolvedSchema.getAllOf).exists(_.asScala.nonEmpty)
          val isObjectSchema = resolvedSchema.isInstanceOf[ObjectSchema]
          val fileContent: String = if (hasAllOf && !isObjectSchema) {
            val objSchema = new ObjectSchema()
            objSchema.setAllOf(resolvedSchema.getAllOf)
            objSchema.setProperties(resolvedSchema.getProperties)
            objSchema.setRequired(resolvedSchema.getRequired)
            generateCaseClass(scalaTypeName, objSchema, modelsPackage, openApi, schemas, ListBuffer("import io.circe.{Decoder, Encoder}"), None, modelsPackage)
          } else {
            generateSingleModelFileContent(
              scalaTypeName,
              resolvedSchema,
              modelsPackage,
              openApi,
              schemas,
              None,
              modelsPackage
            )
          }
          if (fileContent.nonEmpty) Some(scalaTypeName -> fileContent) else None
        }
      }
    }
  }

  /**
   * Generate Scala model files and write them to disk.
   * This is a side-effecting method that writes files using the generated content.
   *
   * @param openApi The parsed OpenAPI specification
   * @param basePackageName The base Scala package for generated code
   * @param outputDir The output directory for generated files
   */
  def generateModelFiles(openApi: OpenAPI, basePackageName: String, outputDir: File): Unit = {
    val files = generateModels(openApi, basePackageName)
    val modelsPackage = if (basePackageName.isEmpty || basePackageName == ".") defaultModelsPackage else s"$basePackageName.models"

    // Generate model files
    files.foreach { case (fileName, content) =>
      FileUtils.writeFile(outputDir, modelsPackage, fileName, content)
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
    allSchemas: Map[String, Schema[_]],
    sealedTraitParentOpt: Option[String] = None,
    sealedTraitPackage: String = ""
  ): String = {
    val imports = ListBuffer("import io.circe.{Codec, Decoder, Encoder}")
    val description = Option(schema.getDescription).filter(_.nonEmpty).map(d => s"/**\n * ${d.replace("\n", "\n * ").replace("*/", "* /")}\n */\n").getOrElse("")

    val definition: String = schema match {
      case s if s.getEnum != null && !s.getEnum.isEmpty =>
        generateEnum(scalaTypeName, s, imports)
      case s if s.getDiscriminator != null || (s.getOneOf != null && !s.getOneOf.isEmpty && s.getOneOf.asScala.exists(oneOfSchema => oneOfSchema.get$ref != null)) =>
        if (s.getDiscriminator != null) {
          generateSealedTrait(scalaTypeName, s, imports, modelsPackage)
        } else {
          println(s"Warning: Schema $scalaTypeName has oneOf but no discriminator. Generating as a simple case class or placeholder.")
          if (s.getProperties != null && !s.getProperties.isEmpty) {
            generateCaseClass(scalaTypeName, s.asInstanceOf[ObjectSchema], modelsPackage, openApi, allSchemas, imports, sealedTraitParentOpt, sealedTraitPackage)
          } else {
            s"// TODO: Implement oneOf for $scalaTypeName (no discriminator, no properties)\ncase class $scalaTypeName() // Placeholder"
          }
        }
      case s: ObjectSchema =>
        // Always generate a case class if there are any properties or allOf (even if direct properties are empty)
        if ((s.getProperties != null && !s.getProperties.isEmpty) || (s.getAllOf != null && !s.getAllOf.isEmpty)) {
          generateCaseClass(scalaTypeName, s, modelsPackage, openApi, allSchemas, imports, sealedTraitParentOpt, sealedTraitPackage)
        } else if (s.getAdditionalProperties != null) {
          generateMapOpaqueType(scalaTypeName, s, modelsPackage, openApi, allSchemas, imports)
        } else {
          imports += "import io.circe.JsonObject"
          s"case class $scalaTypeName()\n|\n|object $scalaTypeName {\n|  implicit val enc: Encoder[$scalaTypeName] = deriveEncoder\n|  implicit val dec: Decoder[$scalaTypeName] = deriveDecoder\n|}".stripMargin
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
      s"""package $modelsPackage
      |${imports.distinct.sorted.mkString("\n")}
      |$description$definition
      |""".stripMargin
    }
  }

  private def generatePrimitiveOpaqueType(
    typeName: String,
    underlyingScalaType: String,
    imports: ListBuffer[String]
  ): String = {
    // --- Force all floating point types to BigDecimal ---
    val finalUnderlyingType = underlyingScalaType match {
      case "Float" | "Double" =>
        imports += "import scala.math.BigDecimal"
        "BigDecimal"
      case other => other
    }
    val (encoderLine, decoderLine) = finalUnderlyingType match {
      case "String"     => (s"Encoder.encodeString.contramap(_.value)", s"Decoder.decodeString.map($typeName.apply)")
      case "Int"        => (s"Encoder.encodeInt.contramap(_.value)", s"Decoder.decodeInt.map($typeName.apply)")
      case "Long"       => (s"Encoder.encodeLong.contramap(_.value)", s"Decoder.decodeLong.map($typeName.apply)")
      case "Boolean"    => (s"Encoder.encodeBoolean.contramap(_.value)", s"Decoder.decodeBoolean.map($typeName.apply)")
      case "Float" | "Double" | "BigDecimal" =>
        imports += "import scala.math.BigDecimal"
        (s"Encoder.encodeBigDecimal.contramap(_.value)", s"Decoder.decodeBigDecimal.map($typeName.apply)")
      case "java.time.LocalDate" | "LocalDate" =>
        imports += "import java.time.LocalDate"
        imports += "import java.time.format.DateTimeFormatter"
        imports += "import scala.util.Try"
        (s"Encoder.encodeString.contramap(_.format(DateTimeFormatter.ISO_LOCAL_DATE))",
         s"Decoder.decodeString.emapTry(str => Try(LocalDate.parse(str, DateTimeFormatter.ISO_LOCAL_DATE)).map($typeName.apply))")
      case "java.time.OffsetDateTime" | "OffsetDateTime" =>
        imports += "import java.time.OffsetDateTime"
        imports += "import java.time.format.DateTimeFormatter"
        imports += "import scala.util.Try"
        (s"Encoder.encodeString.contramap(_.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))",
         s"Decoder.decodeString.emap { str =>\n  Try(OffsetDateTime.parse(str, DateTimeFormatter.ISO_OFFSET_DATE_TIME))\n    .toEither\n    .left.map(_.getMessage)\n    .map($typeName.apply)\n}")
      case "java.util.UUID" | "UUID" =>
        imports += "import java.util.UUID"
        imports += "import scala.util.Try"
        (s"Encoder.encodeString.contramap(_.toString)",
         s"Decoder.decodeString.emapTry(str => Try(UUID.fromString(str)).map($typeName.apply))")
      case "Array[Byte]" => // For BinarySchema, often Base64 encoded string in JSON
        imports += "import java.util.Base64"
        (s"Encoder.encodeString.contramap(bytes => Base64.getEncoder.encodeToString(bytes))",
         s"Decoder.decodeString.map(str => $typeName.apply(Base64.getDecoder.decode(str)))")
      case other =>
        println(s"Warning: No specific Circe encoder/decoder $other for opaque type $typeName with underlying $finalUnderlyingType.")
        (s"Encoder.encodeString.contramap(_.toString)", s"Decoder.decodeString.map(s => $typeName.apply(s.asInstanceOf[$finalUnderlyingType])) // FIXME: May not work")
    }

    s"""opaque type $typeName = $finalUnderlyingType

object $typeName {
  def apply(value: $finalUnderlyingType): $typeName = value

  extension (t: $typeName)
    def value: $finalUnderlyingType = t

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
    imports: ListBuffer[String],
    sealedTraitParentOpt: Option[String] = None,
    sealedTraitPackage: String = ""
  ): String = {
    imports += "import io.circe.generic.semiauto.*"
    // --- NEW: Add import for sealed trait if needed ---
    sealedTraitParentOpt.foreach { parent =>
      if (sealedTraitPackage.nonEmpty && sealedTraitPackage != modelsPackage) {
        imports += s"import $sealedTraitPackage.$parent"
      }
    }

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

    // --- NEW: Add extends clause if sealedTraitParentOpt is defined ---
    val extendsStr = sealedTraitParentOpt.map(parent => s" extends $parent").getOrElse("")

    if (combinedProperties.isEmpty) {
      s"""case class $typeName()$extendsStr
        |
        |object $typeName {}""".stripMargin
    } else {
      imports += "import io.circe.generic.semiauto.*"
      val opaqueTypesStr = if (fieldOpaqueTypes.nonEmpty) fieldOpaqueTypes.mkString("\n\n") + "\n\n" else ""
      s"""$opaqueTypesStr
case class $typeName(
${fieldsStr.split("\n").map(l => if(l.trim.startsWith("/**")) l else "  " + l).mkString("\n")}
)$extendsStr

object $typeName:
  given codec: Codec.AsObject[$typeName] = deriveCodec[$typeName]${if (!extendsStr.isEmpty) {
    s"\n\n  // ADT encoders/decoders\n  given adtEncoder: Encoder[${sealedTraitParentOpt.get}] = codec.asInstanceOf[Encoder[${typeName}]]\n  given adtDecoder: Decoder[$typeName] = codec"
  } else ""}
"""
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
    imports: ListBuffer[String],
    modelsPackage: String
  ): String = {
    imports += "import io.circe.syntax.*"

    val disc = schema.getDiscriminator
    if (disc == null) {
      return s"// Sealed trait $traitName generation skipped: No discriminator found."
    }
    val discriminatorPropertyJsonName = disc.getPropertyName // Use original JSON name for lookup
    val discriminatorPropertyScalaName = ScalaNames.sanitize(ScalaNames.toFieldName(discriminatorPropertyJsonName)) match {
      case "type" => "`type`" // Backtick the 'type' keyword
      case other => other
    }

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
      s"case t: $concreteTypeName => t.asJsonObject"
    }.mkString("\n    ")

    s"""sealed trait $traitName {
  def $discriminatorPropertyScalaName: String // The discriminator property
}

object $traitName {
  given (using enc: Encoder.AsObject[$traitName]): Encoder[$traitName] = enc

  given Decoder[$traitName] = Decoder.instance { c =>
    c.downField("${discriminatorPropertyJsonName}").as[String].flatMap {
      $decoderCases
      case other => Left(io.circe.DecodingFailure(s"Unknown value '$${other}' for discriminator '${discriminatorPropertyJsonName}' in $traitName", c.history))
    }
  }

  given Encoder.AsObject[$traitName] = Encoder.AsObject.instance {
    $encoderCases
  }
}
"""
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
        case Some("float") => imports += "import scala.math.BigDecimal"; "BigDecimal"
        case Some("double") => imports += "import scala.math.BigDecimal"; "BigDecimal"
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
            imports += "import io.circe.*"
            "Json"
        }
    }
    if (isPropertyNullable) s"Option[$coreType]" else coreType
  }

  private def generateCaseClassWithOpaqueTypes(
    typeName: String,
    schema: Schema[_],
    modelsPackage: String,
    openApi: OpenAPI,
    allSchemas: Map[String, Schema[_]],
    sealedTraitParentOpt: Option[String]
  ): (String, Seq[String]) = {
    val imports = ListBuffer[String](
      "import io.circe.*",
      "import io.circe.generic.semiauto.*",
      "import cats.syntax.functor.*",
    )

    val allImports = imports.distinct.sorted.mkString("\n")

    // --- Find parent schema and its properties via allOf ---
    val allOfSchemas = Option(schema.getAllOf).map(_.asScala.toList).getOrElse(List.empty)
    val parentProperties = allOfSchemas.flatMap { refSchema =>
      if (refSchema.get$ref != null) {
        val refName = refSchema.get$ref.substring(refSchema.get$ref.lastIndexOf('/') + 1)
        val parentSchema = resolveSchema(refSchema, openApi)
        // Get parent's property names and their opaque type names
        Option(parentSchema.getProperties).map(_.asScala.toMap).getOrElse(Map.empty).map {
          case (propName, propSch) =>
            val fieldName = ScalaNames.sanitize(ScalaNames.toFieldName(propName))
            fieldName -> (propSch, s"${ScalaNames.toTypeName(refName)}${ScalaNames.toTypeName(fieldName)}")
        }
      } else Map.empty[String, (Schema[_], String)]
    }.toMap

    val localPropertiesMap = Option(schema.getProperties).map(_.asScala.toMap).getOrElse(Map.empty[String, Schema[_]])
    val requiredFields = Option(schema.getRequired).map(_.asScala.toSet).getOrElse(Set.empty[String])
    val localPropertyDefs = localPropertiesMap.map { case (propName, propSchema) =>
      val isNullable = Option(propSchema.getNullable).exists(identity) || !requiredFields.contains(propName)
      ScalaNames.sanitize(ScalaNames.toFieldName(propName)) -> (propSchema, isNullable)
    }

    val fieldOpaqueTypes = ListBuffer[String]()
    // Combine both inherited and local fields for the constructor
    val fieldsWithTypes = (parentProperties.map { case (fieldName, (propSchema, parentOpaqueName)) =>
      // Use the parent's opaque type for inherited fields
      (fieldName, parentOpaqueName)
    } ++ localPropertyDefs.map { case (fieldName, (propSchema, isNullable)) =>
      val shouldGenerateOpaqueType = isPrimitive(propSchema)
      val fieldType = if (shouldGenerateOpaqueType) {
        // Check if this field exists in parent with an opaque type
        parentProperties.get(fieldName) match {
          case Some((_, parentOpaqueName)) =>
            // Use parent's opaque type
            if (isNullable) s"Option[$parentOpaqueName]" else parentOpaqueName
          case None =>
            // Generate new opaque type for this field
            val underlyingType = getScalaType(propSchema, modelsPackage, openApi, allSchemas, isPropertyNullable = false, imports)
            val opaqueTypeName = s"${typeName}${ScalaNames.toTypeName(fieldName)}"
            fieldOpaqueTypes += generatePrimitiveOpaqueType(opaqueTypeName, underlyingType, imports)
            if (isNullable) s"Option[$opaqueTypeName]" else opaqueTypeName
        }
      } else {
        getScalaType(propSchema, modelsPackage, openApi, allSchemas, isPropertyNullable = isNullable, imports)
      }
      (fieldName, fieldType)
    }).toMap

    val fieldsStr = fieldsWithTypes.map { case (fieldName, fieldType) =>
      s"  $fieldName: $fieldType" + (if (fieldType.startsWith("Option")) " = None" else "")
    }.mkString(",\n")
    val extendsStr = sealedTraitParentOpt.map(parent => s" extends $parent").getOrElse("")
    val opaqueTypesStr = if (fieldOpaqueTypes.nonEmpty) fieldOpaqueTypes.mkString("\n\n") + "\n\n" else ""
    val code = s"""$allImports

$opaqueTypesStr
// $typeName case class
final case class $typeName(
$fieldsStr
)$extendsStr

object $typeName {
  given codec: Codec.AsObject[$typeName] = deriveCodec[$typeName]${if (!extendsStr.isEmpty) {
    s"\n\n  // ADT encoders/decoders\n  given adtEncoder: Encoder[${typeName}] = codec.asInstanceOf[Encoder[${typeName}]]\n  given adtDecoder: Decoder[$typeName] = codec"
  } else ""}
}
"""
    (code, imports.distinct.toSeq)
  }
}
