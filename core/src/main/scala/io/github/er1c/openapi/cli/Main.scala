package io.github.er1c.openapi.cli

import io.github.er1c.openapi.parser.OpenApiParser
import io.github.er1c.openapi.generator.{ClientProjectGenerator, ModelGenerator}
import java.io.File
import scala.util.{Failure, Success, Try}

case class CliConfig(
    specFile: File = new File("."),
    outputDir: File = new File("."),
    packageName: String = "io.github.er1c.generated",
    projectName: String = "",
    scalaVersion: String = "3.3.1",
    generateFullProject: Boolean = false
)

object Main {
  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[CliConfig]("openapi-client-generator") {
      head("openapi-client-generator", "0.1.0")

      opt[File]("spec")
        .required()
        .valueName("<file>")
        .action((x, c) => c.copy(specFile = x))
        .text("OpenAPI specification file (YAML or JSON) is required")
        .validate(f =>
          if (f.exists && f.isFile) success
          else failure(s"Spec file ${f.getPath} does not exist or is not a file")
        )

      opt[File]("outputDir")
        .required()
        .valueName("<dir>")
        .action((x, c) => c.copy(outputDir = x))
        .text("Output directory for generated code is required")
        .validate(d =>
          if (d.exists && d.isDirectory) success
          else if (!d.exists) {
            // Try to create it
            try {
              d.mkdirs()
              success
            } catch {
              case e: Throwable => failure(s"Could not create output directory ${d.getPath}: ${e.getMessage}")
            }
          }
          else failure(s"Output directory ${d.getPath} is not a directory")
        )

      opt[String]("packageName")
        .required()
        .valueName("<package>")
        .action((x, c) => c.copy(packageName = x))
        .text("Base package name for generated code is required")

      opt[String]("projectName")
        .valueName("<name>")
        .action((x, c) => c.copy(projectName = x))
        .text("Project name for generated client (required when --fullProject is used)")

      opt[String]("scalaVersion")
        .valueName("<version>")
        .action((x, c) => c.copy(scalaVersion = x))
        .text("Scala version to use for generated client (defaults to 3.3.1)")

      opt[Unit]("fullProject")
        .action((_, c) => c.copy(generateFullProject = true))
        .text("Generate a complete client project with build files")

      help("help").text("prints this usage text")
    }

    parser.parse(args, CliConfig()) match {
      case Some(config) =>
        println(s"Parsed configuration: ${config}")
        try {
          val openApi = OpenApiParser.parseFromFile(config.specFile.getAbsolutePath())
          println(s"Successfully parsed OpenAPI spec: ${config.specFile.getAbsolutePath()}")

          if (config.generateFullProject) {
            if (config.projectName.isEmpty) {
              System.err.println("Error: --projectName is required when using --fullProject")
              System.exit(1)
            }

            println(s"Generating full client project '${config.projectName}' in ${config.outputDir.getAbsolutePath}")
            ClientProjectGenerator(
              basePackage = config.packageName,
              projectName = config.projectName,
              targetDirectory = new File(config.outputDir, config.projectName),
              scalaVersion = config.scalaVersion
            ).generate(openApi) match {
              case Success(_) =>
                println(s"Successfully generated client project in ${config.outputDir.getAbsolutePath}/${config.projectName}")
              case Failure(e) =>
                System.err.println(s"Error generating client project: ${e.getMessage}")
                e.printStackTrace()
                System.exit(1)
            }
          } else {
            // Generate just the models
            println(s"Generating models in package ${config.packageName}.models into ${config.outputDir.getAbsolutePath}")
            ModelGenerator.generateModelFiles(openApi, config.packageName, config.outputDir)
            println("Model generation complete.")
          }
        } catch {
          case e: IllegalArgumentException =>
            System.err.println(s"Error parsing OpenAPI spec: ${e.getMessage}")
            System.exit(1)
          case e: Throwable =>
            System.err.println(s"An unexpected error occurred: ${e.getMessage}")
            e.printStackTrace()
            System.exit(1)
        }
      case None =>
        // arguments are bad, error message will have been displayed
        System.exit(1)
    }
  }
}
