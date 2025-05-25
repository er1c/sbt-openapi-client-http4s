package io.github.er1c.generated.cosmology.models

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

opaque type WhiteDwarfCarboninfusion = String

object WhiteDwarfCarboninfusion {
  def apply(value: String): WhiteDwarfCarboninfusion = value

  extension (t: WhiteDwarfCarboninfusion)
    def value: String = t

  given Encoder[WhiteDwarfCarboninfusion] = Encoder.encodeString.contramap(_.value)
  given Decoder[WhiteDwarfCarboninfusion] = Decoder.decodeString.map(WhiteDwarfCarboninfusion.apply)
}


case class WhiteDwarf(
    carboninfusion: WhiteDwarfCarboninfusion,
    `type`: String
)
object WhiteDwarf {
  implicit val encoder: Encoder[WhiteDwarf] = deriveEncoder[WhiteDwarf]
  implicit val decoder: Decoder[WhiteDwarf] = deriveDecoder[WhiteDwarf]
}