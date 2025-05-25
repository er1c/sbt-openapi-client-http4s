package io.github.er1c.generated.petstore.models

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

opaque type CategoryId = Long

object CategoryId {
  def apply(value: Long): CategoryId = value

  extension (t: CategoryId)
    def value: Long = t

  given Encoder[CategoryId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[CategoryId] = Decoder.decodeLong.map(CategoryId.apply)
}

opaque type CategoryName = String

object CategoryName {
  def apply(value: String): CategoryName = value

  extension (t: CategoryName)
    def value: String = t

  given Encoder[CategoryName] = Encoder.encodeString.contramap(_.value)
  given Decoder[CategoryName] = Decoder.decodeString.map(CategoryName.apply)
}


case class Category(
    id: Option[CategoryId],
    name: Option[CategoryName]
)
object Category {
  implicit val encoder: Encoder[Category] = deriveEncoder[Category]
  implicit val decoder: Decoder[Category] = deriveDecoder[Category]
}