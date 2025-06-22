package io.github.er1c.generated.models
import io.circe.generic.semiauto.*
import io.circe.{Codec, Decoder, Encoder}
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import scala.util.Try
opaque type OrderShipDate = OffsetDateTime

object OrderShipDate {
  def apply(value: OffsetDateTime): OrderShipDate = value

  extension (t: OrderShipDate)
    def value: OffsetDateTime = t

  given Encoder[OrderShipDate] = Encoder.encodeString.contramap(_.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
  given Decoder[OrderShipDate] = Decoder.decodeString.emap { str =>
  Try(OffsetDateTime.parse(str, DateTimeFormatter.ISO_OFFSET_DATE_TIME))
    .toEither
    .left.map(_.getMessage)
    .map(OrderShipDate.apply)
}
}

opaque type OrderQuantity = Int

object OrderQuantity {
  def apply(value: Int): OrderQuantity = value

  extension (t: OrderQuantity)
    def value: Int = t

  given Encoder[OrderQuantity] = Encoder.encodeInt.contramap(_.value)
  given Decoder[OrderQuantity] = Decoder.decodeInt.map(OrderQuantity.apply)
}

opaque type OrderPetId = Long

object OrderPetId {
  def apply(value: Long): OrderPetId = value

  extension (t: OrderPetId)
    def value: Long = t

  given Encoder[OrderPetId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[OrderPetId] = Decoder.decodeLong.map(OrderPetId.apply)
}

opaque type OrderId = Long

object OrderId {
  def apply(value: Long): OrderId = value

  extension (t: OrderId)
    def value: Long = t

  given Encoder[OrderId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[OrderId] = Decoder.decodeLong.map(OrderId.apply)
}

opaque type OrderComplete = Boolean

object OrderComplete {
  def apply(value: Boolean): OrderComplete = value

  extension (t: OrderComplete)
    def value: Boolean = t

  given Encoder[OrderComplete] = Encoder.encodeBoolean.contramap(_.value)
  given Decoder[OrderComplete] = Decoder.decodeBoolean.map(OrderComplete.apply)
}

opaque type OrderStatus = String

object OrderStatus {
  def apply(value: String): OrderStatus = value

  extension (t: OrderStatus)
    def value: String = t

  given Encoder[OrderStatus] = Encoder.encodeString.contramap(_.value)
  given Decoder[OrderStatus] = Decoder.decodeString.map(OrderStatus.apply)
}


case class Order(
    shipDate: Option[OrderShipDate],
    quantity: Option[OrderQuantity],
    petId: Option[OrderPetId],
    id: Option[OrderId],
    complete: Option[OrderComplete],
/** Order Status */
    status: Option[OrderStatus]
)

object Order:
  given codec: Codec.AsObject[Order] = deriveCodec[Order]

