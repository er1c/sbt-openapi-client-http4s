import sbt.Keys._
import sbt._

ThisBuild / organization := "io.github.er1c"
ThisBuild / scalaVersion := "2.12.20" // Using Scala 2.12 for the code generator
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-unchecked",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    //"-Ywarn-unused",
    "-Xfatal-warnings"
  ),
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.2.17" % Test
  )
)

lazy val core = project
  .in(file("core"))
  .settings(
    name := "openapi-client-core",
    commonSettings,
    mainClass := Some("io.github.er1c.openapi.cli.Main"),
    libraryDependencies ++= Seq(
      // Swagger/OpenAPI specification parser
      "io.swagger.parser.v3" % "swagger-parser" % "2.1.28",
      // For command-line option parsing
      "com.github.scopt" %% "scopt" % "4.1.0",
      // For JSON handling
      "io.circe" %% "circe-core" % "0.14.6",
      "io.circe" %% "circe-generic" % "0.14.6",
      "io.circe" %% "circe-generic-extras" % "0.14.3",
      "io.circe" %% "circe-parser" % "0.14.6",
      "io.circe" %% "circe-derivation" % "0.13.0-M5" % Provided,
      "org.typelevel" %% "cats-core" % "2.10.0",
    )
  )

// Root project that aggregates the core and future plugin projects
lazy val root = project
  .in(file("."))
  .aggregate(core)
  .settings(
    name := "sbt-openapi-client-http4s",
    publish / skip := true
  )
