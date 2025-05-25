package io.github.er1c.generated.petstore.models

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

opaque type TagId = Long

object TagId {
  def apply(value: Long): TagId = value

  extension (t: TagId)
    def value: Long = t

  given Encoder[TagId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[TagId] = Decoder.decodeLong.map(TagId.apply)
}

opaque type TagName = String

object TagName {
  def apply(value: String): TagName = value

  extension (t: TagName)
    def value: String = t

  given Encoder[TagName] = Encoder.encodeString.contramap(_.value)
  given Decoder[TagName] = Decoder.decodeString.map(TagName.apply)
}


case class Tag(
    id: Option[TagId],
    name: Option[TagName]
)
object Tag {
  implicit val encoder: Encoder[Tag] = deriveEncoder[Tag]
  implicit val decoder: Decoder[Tag] = deriveDecoder[Tag]
}