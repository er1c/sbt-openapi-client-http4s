package io.github.er1c.generated.models

import cats.syntax.functor.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import java.util.UUID
import scala.util.Try

sealed trait CelestialBody {
  def mass: CelestialBodyMass
}

opaque type CelestialBodyMass = BigDecimal

object CelestialBodyMass {
  def apply(value: BigDecimal): CelestialBodyMass = value

  extension (t: CelestialBodyMass)
    def value: BigDecimal = t

  given Encoder[CelestialBodyMass] = Encoder.encodeBigDecimal.contramap(_.value)
  given Decoder[CelestialBodyMass] = Decoder.decodeBigDecimal.map(CelestialBodyMass.apply)
}


import cats.syntax.functor.*
import io.circe.*
import io.circe.generic.semiauto.*

opaque type MoonUuid = UUID

object MoonUuid {
  def apply(value: UUID): MoonUuid = value

  extension (t: MoonUuid)
    def value: UUID = t

  given Encoder[MoonUuid] = Encoder.encodeString.contramap(_.toString)
  given Decoder[MoonUuid] = Decoder.decodeString.emapTry(str => Try(UUID.fromString(str)).map(MoonUuid.apply))
}


// Moon case class
final case class Moon(
  mass: CelestialBodyMass,
  uuid: MoonUuid
) extends CelestialBody

object Moon {
  given codec: Codec.AsObject[Moon] = deriveCodec[Moon]

  // ADT encoders/decoders
  given adtEncoder: Encoder[Moon] = codec.asInstanceOf[Encoder[Moon]]
  given adtDecoder: Decoder[Moon] = codec
}


object CelestialBody:
  given Decoder[CelestialBody] = List[Decoder[CelestialBody]](
    summon[Decoder[Moon]].map[CelestialBody](identity)
  ).reduceLeft(_ `or` _)

  given Encoder.AsObject[CelestialBody] = Encoder.AsObject.instance {
    case v: Moon => v.asJsonObject
  }
