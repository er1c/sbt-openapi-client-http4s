package io.github.er1c.generated.cosmology.models

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

opaque type CelestialBodyMass = Double

object CelestialBodyMass {
  def apply(value: Double): CelestialBodyMass = value

  extension (t: CelestialBodyMass)
    def value: Double = t

  given Encoder[CelestialBodyMass] = Encoder.encodeDouble.contramap(_.value)
  given Decoder[CelestialBodyMass] = Decoder.decodeDouble.map(CelestialBodyMass.apply)
}


case class CelestialBody(
    mass: CelestialBodyMass
)
object CelestialBody {
  implicit val encoder: Encoder[CelestialBody] = deriveEncoder[CelestialBody]
  implicit val decoder: Decoder[CelestialBody] = deriveDecoder[CelestialBody]
}