package io.github.er1c.generated.models

import io.circe.parser.*
import io.circe.syntax.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ErrorJsonRoundTripSpec extends AnyFlatSpec with Matchers {
  "Error".should("round-trip JSON serialize/deserialize") in {
    val jsonStr = """{"code":"KmaMUXGrcEHUKHzNQbTQhUaZtcxlaNvNKukkg","message":"WEjLKWUEUMsCQjALXvNNhLArux"}"""
    val decoded = decode[Error](jsonStr)
    decoded.isRight shouldBe true
    val model = decoded.toOption.get
    val encoded = model.asJson.noSpaces
    val decodedAgain = decode[Error](encoded)
    decodedAgain.isRight shouldBe true
    decodedAgain.toOption.get shouldBe model
  }
}
