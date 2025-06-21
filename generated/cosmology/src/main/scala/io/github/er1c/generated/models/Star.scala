package io.github.er1c.generated.models

import cats.syntax.functor.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*

sealed trait Star derives Encoder.AsObject, Decoder {
  def `type`: String // The discriminator property
}

import cats.syntax.functor.*
import io.circe.*
import io.circe.generic.semiauto.*

opaque type RedGiantHeliumfusion = String

object RedGiantHeliumfusion {
  def apply(value: String): RedGiantHeliumfusion = value

  extension (t: RedGiantHeliumfusion)
    def value: String = t

  given Encoder[RedGiantHeliumfusion] = Encoder.encodeString.contramap(_.value)
  given Decoder[RedGiantHeliumfusion] = Decoder.decodeString.map(RedGiantHeliumfusion.apply)
}


// RedGiant case class
final case class RedGiant(
  heliumfusion: RedGiantHeliumfusion,
  `type`: String
) extends Star derives Encoder.AsObject, Decoder

object RedGiant {
  given codec: Codec.AsObject[RedGiant] = deriveCodec[RedGiant]

  // ADT encoders/decoders
  given adtEncoder: Encoder[RedGiant] = codec.asInstanceOf[Encoder[RedGiant]]
  given adtDecoder: Decoder[RedGiant] = codec
}


import cats.syntax.functor.*
import io.circe.*
import io.circe.generic.semiauto.*

opaque type WhiteDwarfCarboninfusion = String

object WhiteDwarfCarboninfusion {
  def apply(value: String): WhiteDwarfCarboninfusion = value

  extension (t: WhiteDwarfCarboninfusion)
    def value: String = t

  given Encoder[WhiteDwarfCarboninfusion] = Encoder.encodeString.contramap(_.value)
  given Decoder[WhiteDwarfCarboninfusion] = Decoder.decodeString.map(WhiteDwarfCarboninfusion.apply)
}


// WhiteDwarf case class
final case class WhiteDwarf(
  carboninfusion: WhiteDwarfCarboninfusion,
  `type`: String
) extends Star derives Encoder.AsObject, Decoder

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
) extends Star derives Encoder.AsObject, Decoder

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
