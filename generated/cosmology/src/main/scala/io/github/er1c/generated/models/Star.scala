package io.github.er1c.generated.models

import cats.syntax.functor.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*

sealed trait Star {
  def `type`: String // The discriminator property
}

import cats.syntax.functor.*
import io.circe.*
import io.circe.generic.semiauto.*

opaque type RedGiantHeliumFusion = String

object RedGiantHeliumFusion {
  def apply(value: String): RedGiantHeliumFusion = value

  extension (t: RedGiantHeliumFusion)
    def value: String = t

  given Encoder[RedGiantHeliumFusion] = Encoder.encodeString.contramap(_.value)
  given Decoder[RedGiantHeliumFusion] = Decoder.decodeString.map(RedGiantHeliumFusion.apply)
}


// RedGiant case class
final case class RedGiant(
  heliumFusion: RedGiantHeliumFusion,
  `type`: String
) extends Star

object RedGiant {
  given codec: Codec.AsObject[RedGiant] = deriveCodec[RedGiant]

  // ADT encoders/decoders
  given adtEncoder: Encoder[RedGiant] = codec.asInstanceOf[Encoder[RedGiant]]
  given adtDecoder: Decoder[RedGiant] = codec
}


import cats.syntax.functor.*
import io.circe.*
import io.circe.generic.semiauto.*

opaque type WhiteDwarfCarbonInfusion = String

object WhiteDwarfCarbonInfusion {
  def apply(value: String): WhiteDwarfCarbonInfusion = value

  extension (t: WhiteDwarfCarbonInfusion)
    def value: String = t

  given Encoder[WhiteDwarfCarbonInfusion] = Encoder.encodeString.contramap(_.value)
  given Decoder[WhiteDwarfCarbonInfusion] = Decoder.decodeString.map(WhiteDwarfCarbonInfusion.apply)
}


// WhiteDwarf case class
final case class WhiteDwarf(
  carbonInfusion: WhiteDwarfCarbonInfusion,
  `type`: String
) extends Star

object WhiteDwarf {
  given codec: Codec.AsObject[WhiteDwarf] = deriveCodec[WhiteDwarf]

  // ADT encoders/decoders
  given adtEncoder: Encoder[WhiteDwarf] = codec.asInstanceOf[Encoder[WhiteDwarf]]
  given adtDecoder: Decoder[WhiteDwarf] = codec
}


import cats.syntax.functor.*
import io.circe.*
import io.circe.generic.semiauto.*

opaque type SupernovaPhase = String

object SupernovaPhase {
  def apply(value: String): SupernovaPhase = value

  extension (t: SupernovaPhase)
    def value: String = t

  given Encoder[SupernovaPhase] = Encoder.encodeString.contramap(_.value)
  given Decoder[SupernovaPhase] = Decoder.decodeString.map(SupernovaPhase.apply)
}


// Supernova case class
final case class Supernova(
  phase: SupernovaPhase,
  `type`: String
) extends Star

object Supernova {
  given codec: Codec.AsObject[Supernova] = deriveCodec[Supernova]

  // ADT encoders/decoders
  given adtEncoder: Encoder[Supernova] = codec.asInstanceOf[Encoder[Supernova]]
  given adtDecoder: Decoder[Supernova] = codec
}


object Star:
  given Decoder[Star] = {
    val decoders = List[Decoder[Star]](
      summon[Decoder[RedGiant]].map(identity),
      summon[Decoder[WhiteDwarf]].map(identity),
      summon[Decoder[Supernova]].map(identity)
    )
    decoders.reduceLeft(_ `or` _)
  }

  given Encoder.AsObject[Star] = Encoder.AsObject.instance {
    case v: RedGiant => v.asJsonObject
    case v: WhiteDwarf => v.asJsonObject
    case v: Supernova => v.asJsonObject
  }
