package io.github.er1c.generated.cosmology.models

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}


case class SolarSystem(
    galaxy: Galaxy
)
object SolarSystem {
  implicit val encoder: Encoder[SolarSystem] = deriveEncoder[SolarSystem]
  implicit val decoder: Decoder[SolarSystem] = deriveDecoder[SolarSystem]
}