package io.github.er1c.generated.models
import io.circe.generic.semiauto.*
import io.circe.{Codec, Decoder, Encoder}
opaque type ApiResponseCode = Int

object ApiResponseCode {
  def apply(value: Int): ApiResponseCode = value

  extension (t: ApiResponseCode)
    def value: Int = t

  given Encoder[ApiResponseCode] = Encoder.encodeInt.contramap(_.value)
  given Decoder[ApiResponseCode] = Decoder.decodeInt.map(ApiResponseCode.apply)
}

opaque type ApiResponseType = String

object ApiResponseType {
  def apply(value: String): ApiResponseType = value

  extension (t: ApiResponseType)
    def value: String = t

  given Encoder[ApiResponseType] = Encoder.encodeString.contramap(_.value)
  given Decoder[ApiResponseType] = Decoder.decodeString.map(ApiResponseType.apply)
}

opaque type ApiResponseMessage = String

object ApiResponseMessage {
  def apply(value: String): ApiResponseMessage = value

  extension (t: ApiResponseMessage)
    def value: String = t

  given Encoder[ApiResponseMessage] = Encoder.encodeString.contramap(_.value)
  given Decoder[ApiResponseMessage] = Decoder.decodeString.map(ApiResponseMessage.apply)
}


case class ApiResponse(
    code: Option[ApiResponseCode],
    `type`: Option[ApiResponseType],
    message: Option[ApiResponseMessage]
)

object ApiResponse:
  given codec: Codec.AsObject[ApiResponse] = deriveCodec[ApiResponse]

