ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.1"

val circeVersion = "0.14.6"
val http4sVersion = "0.23.24"
val catsEffectVersion = "3.5.2"
val scalaTestVersion = "3.2.17"

lazy val root = (project in file("."))
  .settings(
    name := "cosmology",
    organization := "io.github.er1c",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.10.0",
      "org.typelevel" %% "cats-effect" % catsEffectVersion,

      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-ember-client" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,

      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,

      "org.scalatest" %% "scalatest" % scalaTestVersion % Test
    ),
    scalacOptions ++= Seq(
      "-source:future",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Xfatal-warnings",
      "-Ykind-projector",
      "-explain"
    )
  )
