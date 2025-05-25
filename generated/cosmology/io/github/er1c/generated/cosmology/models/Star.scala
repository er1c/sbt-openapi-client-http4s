package io.github.er1c.generated.cosmology.models

import io.circe.syntax._
import io.circe.{Decoder, Encoder}

sealed trait Star {
  def type: String // The discriminator property
}

object Star {
  implicit val decoder: Decoder[Star] = Decoder.instance { c =>
    c.downField("type").as[String].flatMap {
      case "RedGiant" => c.as[RedGiant]
      case "Supernova" => c.as[Supernova]
      case "WhiteDwarf" => c.as[WhiteDwarf]
      case other => Left(io.circe.DecodingFailure(s"Unknown value '${other}' for discriminator 'type' in Star", c.history))
    }
  }

  implicit val encoder: Encoder[Star] = Encoder.instance {
    case t: RedGiant => t.asJson
      case t: Supernova => t.asJson
      case t: WhiteDwarf => t.asJson
  }
}