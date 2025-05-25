package io.github.er1c.generated.petstore.models

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import java.time.OffsetDateTime

opaque type OrderQuantity = Int

object OrderQuantity {
  def apply(value: Int): OrderQuantity = value

  extension (t: OrderQuantity)
    def value: Int = t

  given Encoder[OrderQuantity] = Encoder.encodeInt.contramap(_.value)
  given Decoder[OrderQuantity] = Decoder.decodeInt.map(OrderQuantity.apply)
}

opaque type OrderPetid = Long

object OrderPetid {
  def apply(value: Long): OrderPetid = value

  extension (t: OrderPetid)
    def value: Long = t

  given Encoder[OrderPetid] = Encoder.encodeLong.contramap(_.value)
  given Decoder[OrderPetid] = Decoder.decodeLong.map(OrderPetid.apply)
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

opaque type OrderShipdate = OffsetDateTime

object OrderShipdate {
  def apply(value: OffsetDateTime): OrderShipdate = value

  extension (t: OrderShipdate)
    def value: OffsetDateTime = t

  given Encoder[OrderShipdate] = Encoder.encodeString.contramap(_.toString)
  given Decoder[OrderShipdate] = Decoder.decodeString.map(s => OrderShipdate.apply(s.asInstanceOf[OffsetDateTime])) // FIXME: May not work
}


case class Order(
    quantity: Option[OrderQuantity],
    petid: Option[OrderPetid],
    id: Option[OrderId],
    complete: Option[OrderComplete],
/** Order Status */
    status: Option[OrderStatus],
    shipdate: Option[OrderShipdate]
)
object Order {
  implicit val encoder: Encoder[Order] = deriveEncoder[Order]
  implicit val decoder: Decoder[Order] = deriveDecoder[Order]
}