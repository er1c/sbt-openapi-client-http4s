package io.github.er1c.openapi.generator

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

object FileUtils {

  def writeFile(outputDir: File, packageName: String, fileName: String, content: String): Unit = {
    val packagePath = packageName.replace('.', '/')
    val fullDir = Paths.get(outputDir.getAbsolutePath, packagePath)
    Files.createDirectories(fullDir)
    val filePath = fullDir.resolve(s"$fileName.scala")
    Files.write(filePath, content.getBytes(StandardCharsets.UTF_8))
    println(s"Generated file: ${filePath.toAbsolutePath.toString}")
  }

}

