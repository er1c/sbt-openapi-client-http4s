package io.github.er1c.generated.models

import io.circe.parser.*
import io.circe.syntax.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class EllipticalGalaxyJsonRoundTripSpec extends AnyFlatSpec with Matchers {
  "EllipticalGalaxy".should("round-trip JSON serialize/deserialize") in {
    val jsonStr = """{"eccentricity":0.0}"""
    val decoded = decode[EllipticalGalaxy](jsonStr)
    decoded.isRight shouldBe true
    val model = decoded.toOption.get
    val encoded = model.asJson.noSpaces
    val decodedAgain = decode[EllipticalGalaxy](encoded)
    decodedAgain.isRight shouldBe true
    decodedAgain.toOption.get shouldBe model
  }
}
