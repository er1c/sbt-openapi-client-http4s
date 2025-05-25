package io.github.er1c.generated.cosmology.models

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

opaque type SupernovaPhase = String

object SupernovaPhase {
  def apply(value: String): SupernovaPhase = value

  extension (t: SupernovaPhase)
    def value: String = t

  given Encoder[SupernovaPhase] = Encoder.encodeString.contramap(_.value)
  given Decoder[SupernovaPhase] = Decoder.decodeString.map(SupernovaPhase.apply)
}


case class Supernova(
    phase: SupernovaPhase,
    `type`: String
)
object Supernova {
  implicit val encoder: Encoder[Supernova] = deriveEncoder[Supernova]
  implicit val decoder: Decoder[Supernova] = deriveDecoder[Supernova]
}