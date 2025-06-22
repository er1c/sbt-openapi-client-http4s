// Discriminator value used: EllipticalGalaxy
package io.github.er1c.generated.models

import io.circe.parser.*
import io.circe.syntax.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class EllipticalGalaxyJsonRoundTripSpec extends AnyFlatSpec with Matchers {
  "EllipticalGalaxy".should("round-trip JSON serialize/deserialize as subtype") in {
    val jsonStr = """{"eccentricity":992.2904981341685}"""
    val decoded = decode[EllipticalGalaxy](jsonStr)
    decoded.isRight shouldBe true
    val model = decoded.toOption.get
    val encoded = model.asJson.noSpaces
    val decodedAgain = decode[EllipticalGalaxy](encoded)
    decodedAgain.isRight shouldBe true
    decodedAgain.toOption.get shouldBe model
  }

  "EllipticalGalaxy as Galaxy".should("round-trip JSON serialize/deserialize as parent type") in {
    val jsonStr = """{"eccentricity":992.2904981341685}"""
    val decoded = decode[Galaxy](jsonStr)
    decoded.isRight shouldBe true
    val model = decoded.toOption.get
    val encoded = model.asJson.noSpaces
    val decodedAgain = decode[Galaxy](encoded)
    decodedAgain.isRight shouldBe true
    decodedAgain.toOption.get shouldBe model
  }
}
