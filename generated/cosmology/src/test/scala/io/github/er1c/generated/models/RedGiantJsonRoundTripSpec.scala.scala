package io.github.er1c.generated.models

import io.circe.parser.*
import io.circe.syntax.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RedGiantJsonRoundTripSpec extends AnyFlatSpec with Matchers {
  "RedGiant".should("round-trip JSON serialize/deserialize") in {
    val jsonStr = """{"heliumFusion":"Exhaustion","type":"WhiteDwarf"}"""
    val decoded = decode[RedGiant](jsonStr)
    decoded.isRight shouldBe true
    val model = decoded.toOption.get
    val encoded = model.asJson.noSpaces
    val decodedAgain = decode[RedGiant](encoded)
    decodedAgain.isRight shouldBe true
    decodedAgain.toOption.get shouldBe model
  }
}
