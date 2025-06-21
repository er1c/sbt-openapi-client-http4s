package io.github.er1c.generated.models
import io.circe.generic.semiauto.*
import io.circe.{Codec, Decoder, Encoder}
opaque type UserEmail = String

object UserEmail {
  def apply(value: String): UserEmail = value

  extension (t: UserEmail)
    def value: String = t

  given Encoder[UserEmail] = Encoder.encodeString.contramap(_.value)
  given Decoder[UserEmail] = Decoder.decodeString.map(UserEmail.apply)
}

opaque type UserUsername = String

object UserUsername {
  def apply(value: String): UserUsername = value

  extension (t: UserUsername)
    def value: String = t

  given Encoder[UserUsername] = Encoder.encodeString.contramap(_.value)
  given Decoder[UserUsername] = Decoder.decodeString.map(UserUsername.apply)
}

opaque type UserFirstname = String

object UserFirstname {
  def apply(value: String): UserFirstname = value

  extension (t: UserFirstname)
    def value: String = t

  given Encoder[UserFirstname] = Encoder.encodeString.contramap(_.value)
  given Decoder[UserFirstname] = Decoder.decodeString.map(UserFirstname.apply)
}

opaque type UserLastname = String

object UserLastname {
  def apply(value: String): UserLastname = value

  extension (t: UserLastname)
    def value: String = t

  given Encoder[UserLastname] = Encoder.encodeString.contramap(_.value)
  given Decoder[UserLastname] = Decoder.decodeString.map(UserLastname.apply)
}

opaque type UserId = Long

object UserId {
  def apply(value: Long): UserId = value

  extension (t: UserId)
    def value: Long = t

  given Encoder[UserId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[UserId] = Decoder.decodeLong.map(UserId.apply)
}

opaque type UserUserstatus = Int

object UserUserstatus {
  def apply(value: Int): UserUserstatus = value

  extension (t: UserUserstatus)
    def value: Int = t

  given Encoder[UserUserstatus] = Encoder.encodeInt.contramap(_.value)
  given Decoder[UserUserstatus] = Decoder.decodeInt.map(UserUserstatus.apply)
}

opaque type UserPhone = String

object UserPhone {
  def apply(value: String): UserPhone = value

  extension (t: UserPhone)
    def value: String = t

  given Encoder[UserPhone] = Encoder.encodeString.contramap(_.value)
  given Decoder[UserPhone] = Decoder.decodeString.map(UserPhone.apply)
}

opaque type UserPassword = String

object UserPassword {
  def apply(value: String): UserPassword = value

  extension (t: UserPassword)
    def value: String = t

  given Encoder[UserPassword] = Encoder.encodeString.contramap(_.value)
  given Decoder[UserPassword] = Decoder.decodeString.map(UserPassword.apply)
}


case class User(
    email: Option[UserEmail],
    username: Option[UserUsername],
    firstname: Option[UserFirstname],
    lastname: Option[UserLastname],
    id: Option[UserId],
/** User Status */
    userstatus: Option[UserUserstatus],
    phone: Option[UserPhone],
    password: Option[UserPassword]
) derives Encoder.AsObject, Decoder

object User:
  given codec: Codec.AsObject[User] = deriveCodec[User]

