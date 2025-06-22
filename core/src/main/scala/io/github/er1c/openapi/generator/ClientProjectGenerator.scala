package io.github.er1c.openapi.generator

import io.swagger.v3.oas.models.OpenAPI

import java.io.File
import java.nio.file.{Files, Path, Paths}
import scala.util.Try

/**
 * Generates a complete Scala client project including build files and directory structure
 */
class ClientProjectGenerator(
    val basePackage: String,
    val projectName: String,
    val targetDirectory: File,
    val scalaVersion: String = "3.3.1"
) {
  def generate(openApi: OpenAPI): Try[Unit] = Try {
    // Create project directory structure
    createDirectoryStructure()

    // Generate build.sbt
    generateBuildSbt()

    // Generate build.properties
    generateBuildProperties()

    // Create source directory for generated code
    val sourceDir = new File(targetDirectory, "src/main/scala")
    Files.createDirectories(sourceDir.toPath)

    // Generate models and operations into src/main/scala
    ModelGenerator.generateModelFiles(openApi, basePackage, sourceDir)

    // Generate model round-trip tests into src/test/scala
    val testDir = new File(targetDirectory, "src/test/scala")
    Files.createDirectories(testDir.toPath)
    ModelTestGenerator.generateModelTestFiles(openApi, basePackage, testDir)
  }

  private def createDirectoryStructure(): Unit = {
    val directories = List(
      "src/main/scala",
      "src/main/resources",
      "src/test/scala",
      "src/test/resources",
      "project"
    ).map(dir => targetDirectory.toPath.resolve(dir))

    directories.foreach(dir => Files.createDirectories(dir))
  }

  private def generateBuildSbt(): Unit = {
    val content =
      s"""ThisBuild / version := "0.1.0-SNAPSHOT"
         |ThisBuild / scalaVersion := "${scalaVersion}"
         |
         |val circeVersion = "0.14.6"
         |val http4sVersion = "0.23.24"
         |val catsEffectVersion = "3.5.2"
         |val scalaTestVersion = "3.2.17"
         |
         |lazy val root = (project in file("."))
         |  .settings(
         |    name := "$projectName",
         |    organization := "${basePackage.split('.').init.mkString(".")}",
         |    libraryDependencies ++= Seq(
         |      "org.typelevel" %% "cats-core" % "2.10.0",
         |      "org.typelevel" %% "cats-effect" % catsEffectVersion,
         |
         |      "org.http4s" %% "http4s-dsl" % http4sVersion,
         |      "org.http4s" %% "http4s-ember-client" % http4sVersion,
         |      "org.http4s" %% "http4s-circe" % http4sVersion,
         |
         |      "io.circe" %% "circe-core" % circeVersion,
         |      "io.circe" %% "circe-generic" % circeVersion,
         |      "io.circe" %% "circe-parser" % circeVersion,
         |
         |      "org.scalatest" %% "scalatest" % scalaTestVersion % Test
         |    ),
         |    scalacOptions ++= Seq(
         |      "-source:future",
         |      "-deprecation",
         |      "-feature",
         |      "-unchecked",
         |      "-Xfatal-warnings",
         |      "-Ykind-projector",
         |      "-explain"
         |    )
         |  )
         |""".stripMargin

    Files.write(targetDirectory.toPath.resolve("build.sbt"), content.getBytes)
  }

  private def generateBuildProperties(): Unit = {
    val content = "sbt.version=1.9.7"
    Files.write(targetDirectory.toPath.resolve("project/build.properties"), content.getBytes)
  }
}

object ClientProjectGenerator {
  def apply(
      basePackage: String,
      projectName: String,
      targetDirectory: File,
      scalaVersion: String = "3.3.1"
  ): ClientProjectGenerator =
    new ClientProjectGenerator(basePackage, projectName, targetDirectory, scalaVersion)
}
