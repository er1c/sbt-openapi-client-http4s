package io.github.er1c.generated.cosmology.models

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import java.util.UUID

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


case class SpiralGalaxy(
    uuid: Option[SpiralGalaxyUuid],
    armcount: SpiralGalaxyArmcount
)
object SpiralGalaxy {
  implicit val encoder: Encoder[SpiralGalaxy] = deriveEncoder[SpiralGalaxy]
  implicit val decoder: Decoder[SpiralGalaxy] = deriveDecoder[SpiralGalaxy]
}