package io.github.er1c.generated.models
import io.circe.generic.semiauto.*
import io.circe.{Codec, Decoder, Encoder}
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
) derives Encoder.AsObject, Decoder

object Category:
  given codec: Codec.AsObject[Category] = deriveCodec[Category]

