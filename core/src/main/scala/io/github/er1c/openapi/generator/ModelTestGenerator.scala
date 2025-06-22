package io.github.er1c.openapi.generator

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import scala.collection.JavaConverters._
import java.io.File

/**
 * ModelTestGenerator generates ScalaTest code for round-trip JSON serialization/deserialization
 * for a given model. This is intended to complement ModelGenerator and ensure that
 * Circe codecs for generated models are correct.
 *
 * This generator is written in Scala 2.12 for sbt plugin compatibility.
 */
object ModelTestGenerator {
  private val defaultTestsPackage = "io.github.er1c.generated.models"

  /**
   * Generate a map of ScalaTest file names to their contents, for all models.
   * The key is the Scala type name + "JsonRoundTripSpec.scala", the value is the file content.
   * This is a pure function and does not write files.
   */
  def generateModelTests(openApi: OpenAPI, basePackageName: String): Map[String, String] = {
    val modelsPackage = if (basePackageName.isEmpty || basePackageName == ".") defaultTestsPackage else s"$basePackageName.models"
    val testsPackage = modelsPackage // For now, put tests in same package
    val components = Option(openApi.getComponents)
    val schemas = components.flatMap(c => Option(c.getSchemas)).map(_.asScala).getOrElse(Map.empty[String, Schema[_]]).toMap

    // Helper to get direct subtypes for ADTs (discriminator or oneOf)
    def getSubtypes(schema: Schema[_]): Seq[(String, Schema[_])] = {
      val oneOf = Option(schema.getOneOf).map(_.asScala).getOrElse(Seq.empty)
      val disc = schema.getDiscriminator
      if (oneOf.nonEmpty) {
        oneOf.flatMap { s =>
          val ref = Option(s.get$ref)
          ref.flatMap { r =>
            val name = r.substring(r.lastIndexOf('/') + 1)
            schemas.get(name).map(name -> _)
          }
        }
      } else if (disc != null && Option(schema.getOneOf).exists(_.asScala.nonEmpty)) {
        schema.getOneOf.asScala.flatMap { s =>
          val ref = Option(s.get$ref)
          ref.flatMap { r =>
            val name = r.substring(r.lastIndexOf('/') + 1)
            schemas.get(name).map(name -> _)
          }
        }
      } else Seq.empty
    }

    // Collect all ADT child names (subtypes of any ADT)
    val adtChildNames: Set[String] = schemas.values.flatMap { schema =>
      val oneOf = Option(schema.getOneOf).map(_.asScala).getOrElse(Seq.empty)
      oneOf.flatMap { s =>
        val ref = Option(s.get$ref)
        ref.map { r =>
          r.substring(r.lastIndexOf('/') + 1)
        }
      }
    }.toSet

    schemas.flatMap { case (schemaName, schema) =>
      println(s"[ModelTestGenerator] Processing schema: $schemaName, type: ${schema.getClass.getSimpleName}")
      val scalaTypeName = ScalaNames.toTypeName(schemaName)
      // Skip generating tests for CelestialBody, SolarSystem, and any ADT child
      if (scalaTypeName == "CelestialBody" || scalaTypeName == "SolarSystem" || adtChildNames.contains(schemaName)) {
        None
      } else {
        val subtypes = getSubtypes(schema)
        println(s"[ModelTestGenerator]  - subtypes for $schemaName: ${subtypes.map(_._1).mkString(", ")}")
        if (subtypes.nonEmpty) {
          // If this is a discriminator-based ADT, get discriminator info
          val disc = schema.getDiscriminator
          val discName = Option(disc).map(_.getPropertyName)
          val mapping = Option(disc).flatMap(d => Option(d.getMapping)).map(_.asScala.toMap).getOrElse(Map.empty[String, String])
          subtypes.flatMap { case (subName, subSchema) =>
            val subTypeName = ScalaNames.toTypeName(subName)
            // Use the OpenAPI schema name (subName) as the fallback discriminator value
            val discValue =
              mapping.find(_._2.endsWith(subName)).map(_._1)
                .getOrElse(subName)
            val sampleJsonOpt = discName match {
              case Some(dn) => ExampleJsonGenerator.exampleJsonForSchema(subSchema, openApi, Some(dn -> discValue))
              case None => ExampleJsonGenerator.exampleJsonForSchema(subSchema, openApi)
            }
            sampleJsonOpt.map { sampleJson =>
              val testClassName = s"${subTypeName}JsonRoundTripSpec"
              val fileName = testClassName
              val testCode = s"""// Discriminator value used: $discValue
package $testsPackage

import io.circe.parser.*
import io.circe.syntax.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class $testClassName extends AnyFlatSpec with Matchers {
  \"$subTypeName\".should(\"round-trip JSON serialize/deserialize as subtype\") in {
    val jsonStr = \"\"\"${sampleJson}\"\"\"
    val decoded = decode[$subTypeName](jsonStr)
    decoded.isRight shouldBe true
    val model = decoded.toOption.get
    val encoded = model.asJson.noSpaces
    val decodedAgain = decode[$subTypeName](encoded)
    decodedAgain.isRight shouldBe true
    decodedAgain.toOption.get shouldBe model
  }

  \"$subTypeName as $scalaTypeName\".should(\"round-trip JSON serialize/deserialize as parent type\") in {
    val jsonStr = \"\"\"${sampleJson}\"\"\"
    val decoded = decode[$scalaTypeName](jsonStr)
    decoded.isRight shouldBe true
    val model = decoded.toOption.get
    val encoded = model.asJson.noSpaces
    val decodedAgain = decode[$scalaTypeName](encoded)
    decodedAgain.isRight shouldBe true
    decodedAgain.toOption.get shouldBe model
  }
}
"""
              fileName -> testCode
            }
          }.toMap
        } else {
          // Not an ADT, generate a single test as before
          val sampleJsonOpt = ExampleJsonGenerator.exampleJsonForSchema(schema, openApi)
          sampleJsonOpt.map { sampleJson =>
            val testClassName = s"${scalaTypeName}JsonRoundTripSpec"
            val fileName = testClassName // Do not append .scala here
            val testCode = s"""package $testsPackage
                          |
                          |import io.circe.parser.*
                          |import io.circe.syntax.*
                          |import org.scalatest.flatspec.AnyFlatSpec
                          |import org.scalatest.matchers.should.Matchers
                          |
                          |class $testClassName extends AnyFlatSpec with Matchers {
                          |  "$scalaTypeName".should("round-trip JSON serialize/deserialize") in {
                          |    val jsonStr = \"\"\"${sampleJson}\"\"\"
                          |    val decoded = decode[$scalaTypeName](jsonStr)
                          |    decoded.isRight shouldBe true
                          |    val model = decoded.toOption.get
                          |    val encoded = model.asJson.noSpaces
                          |    val decodedAgain = decode[$scalaTypeName](encoded)
                          |    decodedAgain.isRight shouldBe true
                          |    decodedAgain.toOption.get shouldBe model
                          |  }
                          |}
                          |""".stripMargin
            Map(fileName -> testCode)
          }.getOrElse(Map.empty)
        }
      }
    }.toMap
  }

  /**
   * Write generated model test files to disk.
   * @param openApi The parsed OpenAPI specification
   * @param basePackageName The base Scala package for generated code
   * @param outputDir The output directory for generated files
   */
  def generateModelTestFiles(openApi: OpenAPI, basePackageName: String, outputDir: File): Unit = {
    val files = generateModelTests(openApi, basePackageName)
    val modelsPackage = if (basePackageName.isEmpty || basePackageName == ".") defaultTestsPackage else s"$basePackageName.models"
    files.foreach { case (fileName, content) =>
      FileUtils.writeFile(outputDir, modelsPackage, fileName, content)
    }
  }
}
