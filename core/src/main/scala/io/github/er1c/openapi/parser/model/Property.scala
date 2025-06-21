package io.github.er1c.openapi.parser.model

/**
 * Represents a property of a schema model
 *
 * @param name The name of the property
 * @param typeName The Scala type of the property
 * @param required Whether the property is required
 * @param description Optional description of the property
 */
case class Property(
  name: String,
  typeName: String,
  required: Boolean,
  description: Option[String] = None
)
