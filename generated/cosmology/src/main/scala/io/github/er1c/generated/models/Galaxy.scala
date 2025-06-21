package io.github.er1c.generated.models

import cats.syntax.functor.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import java.util.UUID
import scala.math.BigDecimal

sealed trait Galaxy

import cats.syntax.functor.*
import io.circe.*
import io.circe.generic.semiauto.*

opaque type EllipticalGalaxyEccentricity = BigDecimal

object EllipticalGalaxyEccentricity {
  def apply(value: BigDecimal): EllipticalGalaxyEccentricity = value

  extension (t: EllipticalGalaxyEccentricity)
    def value: BigDecimal = t

  given Encoder[EllipticalGalaxyEccentricity] = Encoder.encodeBigDecimal.contramap(_.value)
  given Decoder[EllipticalGalaxyEccentricity] = Decoder.decodeBigDecimal.map(EllipticalGalaxyEccentricity.apply)
}


// EllipticalGalaxy case class
final case class EllipticalGalaxy(
  eccentricity: EllipticalGalaxyEccentricity,
  etcetra: Option[Json] = None
) extends Galaxy derives Encoder.AsObject, Decoder

object EllipticalGalaxy {
  given codec: Codec.AsObject[EllipticalGalaxy] = deriveCodec[EllipticalGalaxy]

  // ADT encoders/decoders
  given adtEncoder: Encoder[EllipticalGalaxy] = codec.asInstanceOf[Encoder[EllipticalGalaxy]]
  given adtDecoder: Decoder[EllipticalGalaxy] = codec
}


import cats.syntax.functor.*
import io.circe.*
import io.circe.generic.semiauto.*

opaque type SpiralGalaxyUuid = UUID

object SpiralGalaxyUuid {
  def apply(value: UUID): SpiralGalaxyUuid = value

  extension (t: SpiralGalaxyUuid)
    def value: UUID = t

  given Encoder[SpiralGalaxyUuid] = Encoder.encodeString.contramap(_.toString)
  given Decoder[SpiralGalaxyUuid] = Decoder.decodeString.map(s => SpiralGalaxyUuid.apply(s.asInstanceOf[UUID])) // FIXME: May not work
}

opaque type SpiralGalaxyArmcount = Int

object SpiralGalaxyArmcount {
  def apply(value: Int): SpiralGalaxyArmcount = value

  extension (t: SpiralGalaxyArmcount)
    def value: Int = t

  given Encoder[SpiralGalaxyArmcount] = Encoder.encodeInt.contramap(_.value)
  given Decoder[SpiralGalaxyArmcount] = Decoder.decodeInt.map(SpiralGalaxyArmcount.apply)
}


// SpiralGalaxy case class
final case class SpiralGalaxy(
  uuid: Option[SpiralGalaxyUuid] = None,
  armcount: SpiralGalaxyArmcount
) extends Galaxy derives Encoder.AsObject, Decoder

object SpiralGalaxy {
  given codec: Codec.AsObject[SpiralGalaxy] = deriveCodec[SpiralGalaxy]

  // ADT encoders/decoders
  given adtEncoder: Encoder[SpiralGalaxy] = codec.asInstanceOf[Encoder[SpiralGalaxy]]
  given adtDecoder: Decoder[SpiralGalaxy] = codec
}


object Galaxy {
  given Decoder[Galaxy] = Decoder[EllipticalGalaxy].widen.or(Decoder[SpiralGalaxy].widen)
  given Encoder[Galaxy] = Encoder.instance {
    case v: EllipticalGalaxy => Encoder[EllipticalGalaxy].apply(v)
    case v: SpiralGalaxy => Encoder[SpiralGalaxy].apply(v)
  }
}
