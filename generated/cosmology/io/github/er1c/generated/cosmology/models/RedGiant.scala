package io.github.er1c.generated.cosmology.models

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

opaque type RedGiantHeliumfusion = String

object RedGiantHeliumfusion {
  def apply(value: String): RedGiantHeliumfusion = value

  extension (t: RedGiantHeliumfusion)
    def value: String = t

  given Encoder[RedGiantHeliumfusion] = Encoder.encodeString.contramap(_.value)
  given Decoder[RedGiantHeliumfusion] = Decoder.decodeString.map(RedGiantHeliumfusion.apply)
}


case class RedGiant(
    heliumfusion: RedGiantHeliumfusion,
    `type`: String
)
object RedGiant {
  implicit val encoder: Encoder[RedGiant] = deriveEncoder[RedGiant]
  implicit val decoder: Decoder[RedGiant] = deriveDecoder[RedGiant]
}