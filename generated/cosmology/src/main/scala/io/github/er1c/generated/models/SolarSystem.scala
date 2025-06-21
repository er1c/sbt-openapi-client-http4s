package io.github.er1c.generated.models
import io.circe.generic.semiauto.*
import io.circe.{Codec, Decoder, Encoder}

case class SolarSystem(
    galaxy: Galaxy
) derives Encoder.AsObject, Decoder

object SolarSystem:
  given codec: Codec.AsObject[SolarSystem] = deriveCodec[SolarSystem]

