package io.github.er1c.openapi.parser.model

/**
 * A model that represents Scala type information generated from OpenAPI schemas.
 * This will be used in the code generation step.
 */
case class ScalaType(
  name: String,
  packageName: String,
  imports: Set[String] = Set.empty,
  properties: Seq[Property] = Seq.empty,
  isEnum: Boolean = false,
  enumValues: Seq[String] = Seq.empty,
  description: Option[String] = None
)