package io.github.er1c.openapi.cli

import io.github.er1c.openapi.parser.OpenApiParser
import io.github.er1c.openapi.generator.ModelGenerator
import java.io.File

case class CliConfig(
    specFile: File = new File("."),
    outputDir: File = new File("."),
    packageName: String = "io.github.er1c.generated"
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

      help("help").text("prints this usage text")
    }

    parser.parse(args, CliConfig()) match {
      case Some(config) =>
        println(s"Parsed configuration: ${config}")
        // Initialize OpenApiParser
        val openApiParser = new OpenApiParser()
        try {
          val openApi = openApiParser.parseFromFile(config.specFile.getAbsolutePath())
          println(s"Successfully parsed OpenAPI spec: ${config.specFile.getAbsolutePath()}")

          // Generate Models
          val modelGenerator = new ModelGenerator()
          println(s"Generating models in package ${config.packageName}.models into ${config.outputDir.getAbsolutePath}")
          modelGenerator.generateModels(openApi, config.packageName, config.outputDir)
          println("Model generation complete.")

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

