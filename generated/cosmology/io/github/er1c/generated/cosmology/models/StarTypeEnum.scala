package io.github.er1c.generated.cosmology.models

import io.circe.Json
import io.circe.{Decoder, Encoder}

enum StarTypeEnum {
  case RedGiant
  case WhiteDwarf
  case Supernova
}
object StarTypeEnum {
  implicit val encoder: Encoder[StarTypeEnum] = Encoder.instance {
    case RedGiant => Json.fromString("RedGiant")
    case WhiteDwarf => Json.fromString("WhiteDwarf")
    case Supernova => Json.fromString("Supernova")
  }
  implicit val decoder: Decoder[StarTypeEnum] = Decoder.decodeString.emap {
    str => str match {
      case "RedGiant" => Right(RedGiant)
      case "WhiteDwarf" => Right(WhiteDwarf)
      case "Supernova" => Right(Supernova)
      case other => Left(s"Unexpected value for enum StarTypeEnum: ${other}")
    }
  }
}