package io.github.er1c.generated.cosmology.models

import io.circe.Json
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

opaque type ElipticalGalaxyEccentricity = Float

object ElipticalGalaxyEccentricity {
  def apply(value: Float): ElipticalGalaxyEccentricity = value

  extension (t: ElipticalGalaxyEccentricity)
    def value: Float = t

  given Encoder[ElipticalGalaxyEccentricity] = Encoder.encodeFloat.contramap(_.value)
  given Decoder[ElipticalGalaxyEccentricity] = Decoder.decodeFloat.map(ElipticalGalaxyEccentricity.apply)
}


case class ElipticalGalaxy(
    eccentricity: ElipticalGalaxyEccentricity,
    etcetra: Option[Json]
)
object ElipticalGalaxy {
  implicit val encoder: Encoder[ElipticalGalaxy] = deriveEncoder[ElipticalGalaxy]
  implicit val decoder: Decoder[ElipticalGalaxy] = deriveDecoder[ElipticalGalaxy]
}