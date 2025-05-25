package io.github.er1c.generated.petstore.models

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

opaque type PetName = String

object PetName {
  def apply(value: String): PetName = value

  extension (t: PetName)
    def value: String = t

  given Encoder[PetName] = Encoder.encodeString.contramap(_.value)
  given Decoder[PetName] = Decoder.decodeString.map(PetName.apply)
}

opaque type PetId = Long

object PetId {
  def apply(value: Long): PetId = value

  extension (t: PetId)
    def value: Long = t

  given Encoder[PetId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[PetId] = Decoder.decodeLong.map(PetId.apply)
}

opaque type PetStatus = String

object PetStatus {
  def apply(value: String): PetStatus = value

  extension (t: PetStatus)
    def value: String = t

  given Encoder[PetStatus] = Encoder.encodeString.contramap(_.value)
  given Decoder[PetStatus] = Decoder.decodeString.map(PetStatus.apply)
}


case class Pet(
    name: PetName,
    tags: Option[List[Tag]],
    photourls: List[String],
    id: Option[PetId],
/** pet status in the store */
    status: Option[PetStatus],
    category: Option[Category]
)
object Pet {
  implicit val encoder: Encoder[Pet] = deriveEncoder[Pet]
  implicit val decoder: Decoder[Pet] = deriveDecoder[Pet]
}