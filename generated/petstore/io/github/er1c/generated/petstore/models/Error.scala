package io.github.er1c.generated.petstore.models

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

opaque type ErrorCode = String

object ErrorCode {
  def apply(value: String): ErrorCode = value

  extension (t: ErrorCode)
    def value: String = t

  given Encoder[ErrorCode] = Encoder.encodeString.contramap(_.value)
  given Decoder[ErrorCode] = Decoder.decodeString.map(ErrorCode.apply)
}

opaque type ErrorMessage = String

object ErrorMessage {
  def apply(value: String): ErrorMessage = value

  extension (t: ErrorMessage)
    def value: String = t

  given Encoder[ErrorMessage] = Encoder.encodeString.contramap(_.value)
  given Decoder[ErrorMessage] = Decoder.decodeString.map(ErrorMessage.apply)
}


case class Error(
    code: ErrorCode,
    message: ErrorMessage
)
object Error {
  implicit val encoder: Encoder[Error] = deriveEncoder[Error]
  implicit val decoder: Decoder[Error] = deriveDecoder[Error]
}