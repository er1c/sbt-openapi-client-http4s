package io.github.er1c.generated.models

import io.circe.parser.*
import io.circe.syntax.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SpiralGalaxyJsonRoundTripSpec extends AnyFlatSpec with Matchers {
  "SpiralGalaxy".should("round-trip JSON serialize/deserialize") in {
    val jsonStr = """{"uuid":"123e4567-e89b-12d3-a456-426614174000","armCount":81}"""
    val decoded = decode[SpiralGalaxy](jsonStr)
    decoded.isRight shouldBe true
    val model = decoded.toOption.get
    val encoded = model.asJson.noSpaces
    val decodedAgain = decode[SpiralGalaxy](encoded)
    decodedAgain.isRight shouldBe true
    decodedAgain.toOption.get shouldBe model
  }
}
