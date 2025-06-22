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

opaque type UserUserStatus = Int

object UserUserStatus {
  def apply(value: Int): UserUserStatus = value

  extension (t: UserUserStatus)
    def value: Int = t

  given Encoder[UserUserStatus] = Encoder.encodeInt.contramap(_.value)
  given Decoder[UserUserStatus] = Decoder.decodeInt.map(UserUserStatus.apply)
}

opaque type UserLastName = String

object UserLastName {
  def apply(value: String): UserLastName = value

  extension (t: UserLastName)
    def value: String = t

  given Encoder[UserLastName] = Encoder.encodeString.contramap(_.value)
  given Decoder[UserLastName] = Decoder.decodeString.map(UserLastName.apply)
}

opaque type UserFirstName = String

object UserFirstName {
  def apply(value: String): UserFirstName = value

  extension (t: UserFirstName)
    def value: String = t

  given Encoder[UserFirstName] = Encoder.encodeString.contramap(_.value)
  given Decoder[UserFirstName] = Decoder.decodeString.map(UserFirstName.apply)
}

opaque type UserId = Long

object UserId {
  def apply(value: Long): UserId = value

  extension (t: UserId)
    def value: Long = t

  given Encoder[UserId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[UserId] = Decoder.decodeLong.map(UserId.apply)
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
/** User Status */
    userStatus: Option[UserUserStatus],
    lastName: Option[UserLastName],
    firstName: Option[UserFirstName],
    id: Option[UserId],
    phone: Option[UserPhone],
    password: Option[UserPassword]
)

object User:
  given codec: Codec.AsObject[User] = deriveCodec[User]

