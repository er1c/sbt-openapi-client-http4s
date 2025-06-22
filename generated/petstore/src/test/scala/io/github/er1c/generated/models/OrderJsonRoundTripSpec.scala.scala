package io.github.er1c.generated.models

import io.circe.parser.*
import io.circe.syntax.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class OrderJsonRoundTripSpec extends AnyFlatSpec with Matchers {
  "Order".should("round-trip JSON serialize/deserialize") in {
    val jsonStr = """{"shipDate":"2023-01-01T12:00:00Z","quantity":75,"petId":23,"id":70,"complete":true,"status":"approved"}"""
    val decoded = decode[Order](jsonStr)
    decoded.isRight shouldBe true
    val model = decoded.toOption.get
    val encoded = model.asJson.noSpaces
    val decodedAgain = decode[Order](encoded)
    decodedAgain.isRight shouldBe true
    decodedAgain.toOption.get shouldBe model
  }
}
