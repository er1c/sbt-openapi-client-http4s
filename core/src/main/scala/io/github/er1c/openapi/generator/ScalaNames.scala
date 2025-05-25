package io.github.er1c.openapi.generator

object ScalaNames {

  /**
   * Converts a name to a valid Scala type name (PascalCase).
   * E.g., "pet_id" -> "PetId", "Pet" -> "Pet"
   */
  def toTypeName(name: String): String = {
    if (name == null || name.isEmpty) "UnnamedType"
    else name.split("[^a-zA-Z0-9]").filter(_.nonEmpty).map(_.capitalize).mkString
  }

  /**
   * Converts a name to a valid Scala field name (camelCase).
   * E.g., "pet_id" -> "petId", "PetName" -> "petName"
   */
  def toFieldName(name: String): String = {
    if (name == null || name.isEmpty) "unnamedField"
    else {
      val parts = name.split("[^a-zA-Z0-9]").filter(_.nonEmpty)
      if (parts.isEmpty) "unnamedField"
      else parts.head.toLowerCase + parts.tail.map(_.capitalize).mkString
    }
  }

  /**
   * Sanitizes a name to be a valid Scala identifier.
   * Replaces invalid characters with underscores and prepends an underscore if it starts with a digit or keyword.
   */
  def sanitize(name: String): String = {
    val sanitized = name.replaceAll("[^a-zA-Z0-9_]", "_")
    if (sanitized.isEmpty) {
      "empty_name"
    } else if (scalaKeywords.contains(sanitized) || sanitized.matches("^[0-9].*")) {
      s"`${sanitized}`"
    } else {
      sanitized
    }
  }

  private val scalaKeywords: Set[String] = Set(
    "abstract", "case", "catch", "class", "def", "do", "else", "enum", "export", "extends",
    "false", "final", "finally", "for", "given", "if", "implicit", "import", "lazy",
    "match", "new", "null", "object", "override", "package", "private", "protected",
    "return", "sealed", "super", "then", "throw", "trait", "true", "try", "type",
    "val", "var", "while", "with", "yield", "_", ":", "=", "=>", "<-", "<:", "<%", ">:", "#", "@"
  )
}

