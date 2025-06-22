// Discriminator value used: SpiralGalaxy
package io.github.er1c.generated.models

import io.circe.parser.*
import io.circe.syntax.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SpiralGalaxyJsonRoundTripSpec extends AnyFlatSpec with Matchers {
  "SpiralGalaxy".should("round-trip JSON serialize/deserialize as subtype") in {
    val jsonStr = """{"uuid":"123e4567-e89b-12d3-a456-426614174000","armCount":22}"""
    val decoded = decode[SpiralGalaxy](jsonStr)
    decoded.isRight shouldBe true
    val model = decoded.toOption.get
    val encoded = model.asJson.noSpaces
    val decodedAgain = decode[SpiralGalaxy](encoded)
    decodedAgain.isRight shouldBe true
    decodedAgain.toOption.get shouldBe model
  }

  "SpiralGalaxy as Galaxy".should("round-trip JSON serialize/deserialize as parent type") in {
    val jsonStr = """{"uuid":"123e4567-e89b-12d3-a456-426614174000","armCount":22}"""
    val decoded = decode[Galaxy](jsonStr)
    decoded.isRight shouldBe true
    val model = decoded.toOption.get
    val encoded = model.asJson.noSpaces
    val decodedAgain = decode[Galaxy](encoded)
    decodedAgain.isRight shouldBe true
    decodedAgain.toOption.get shouldBe model
  }
}
