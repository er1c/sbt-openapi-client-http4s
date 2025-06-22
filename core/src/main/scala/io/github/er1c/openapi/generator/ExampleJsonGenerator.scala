package io.github.er1c.openapi.generator

import io.circe.Json
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media._
import org.scalacheck.Gen
import scala.collection.JavaConverters._

object ExampleJsonGenerator {
  // Entry point: generate a sample JSON string for a schema
  def exampleJsonForSchema(schema: Schema[_], openApi: OpenAPI): Option[String] = {
    val gen = genForSchema(schema, openApi)
    gen.flatMap(_.sample).map(_.noSpaces)
  }

  private def genForSchema(schema: Schema[_], openApi: OpenAPI): Option[Gen[Json]] = {
    schema match {
      case ref if ref.get$ref != null =>
        val refName = ref.get$ref.substring(ref.get$ref.lastIndexOf('/') + 1)
        val resolved = Option(openApi.getComponents)
          .flatMap(c => Option(c.getSchemas).map(_.asScala.toMap))
          .flatMap(_.get(refName))
        resolved.flatMap(genForSchema(_, openApi))
      case obj: ObjectSchema =>
        val props = Option(obj.getProperties).map(_.asScala.toMap).getOrElse(Map.empty)
        val required = Option(obj.getRequired).map(_.asScala.toSet).getOrElse(Set.empty)
        val fields: Seq[(String, Gen[Json])] = props.flatMap { case (name, propSchema) =>
          genForSchema(propSchema, openApi).map(g => name -> g)
        }.toSeq
        if (fields.isEmpty) Some(Gen.const(Json.obj()))
        else Some(Gen.sequence[Seq[(String, Json)], (String, Json)](
          fields.map { case (k, g) => g.map(j => k -> j) }
        ).map(kvs => Json.obj(kvs: _*)))
      case arr: ArraySchema =>
        val itemGen = Option(arr.getItems).flatMap(genForSchema(_, openApi)).getOrElse(Gen.const(Json.Null))
        Some(Gen.listOfN(2, itemGen).map(Json.fromValues))
      case s: StringSchema if s.getEnum != null && !s.getEnum.isEmpty =>
        val values = s.getEnum.asScala.map(_.toString).toSeq
        Some(Gen.oneOf(values).map(Json.fromString))
      case s: StringSchema =>
        Some(Gen.alphaStr.suchThat(_.nonEmpty).map(Json.fromString))
      case s: IntegerSchema =>
        Some(Gen.choose(0, 100).map(Json.fromInt))
      case s: NumberSchema =>
        Some(Gen.chooseNum(0.0, 1000.0).map(d => Json.fromBigDecimal(BigDecimal(d))))
      case s: BooleanSchema =>
        Some(Gen.oneOf(true, false).map(Json.fromBoolean))
      case s: DateSchema =>
        Some(Gen.const(Json.fromString("2023-01-01")))
      case s: DateTimeSchema =>
        Some(Gen.const(Json.fromString("2023-01-01T12:00:00Z")))
      case s: UUIDSchema =>
        Some(Gen.const(Json.fromString("123e4567-e89b-12d3-a456-426614174000")))
      case s: BinarySchema =>
        Some(Gen.const(Json.fromString("aGVsbG8gd29ybGQ=")))
      case _ => None
    }
  }
}

