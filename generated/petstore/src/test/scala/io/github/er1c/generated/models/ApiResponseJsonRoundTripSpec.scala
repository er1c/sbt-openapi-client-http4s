package io.github.er1c.generated.models

import io.circe.parser.*
import io.circe.syntax.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ApiResponseJsonRoundTripSpec extends AnyFlatSpec with Matchers {
  "ApiResponse".should("round-trip JSON serialize/deserialize") in {
    val jsonStr = """{"code":100,"type":"oaazahVKObTgMKPpXxRgjDsLRcKCVcbMifUHgMuSPrwbyEgQdYLNjhmBAfvIizxDNnJqqDYGDhlwjIbkzzWQnYpIPSUz","message":"jexrQTpXVcjRXDLodekvEfELzcuvSUUzRRcHdUiJimWOaycOXtHYHJTTgcdIMpJNWHfCwzKaIqzuyFqVovljYXz"}"""
    val decoded = decode[ApiResponse](jsonStr)
    decoded.isRight shouldBe true
    val model = decoded.toOption.get
    val encoded = model.asJson.noSpaces
    val decodedAgain = decode[ApiResponse](encoded)
    decodedAgain.isRight shouldBe true
    decodedAgain.toOption.get shouldBe model
  }
}
