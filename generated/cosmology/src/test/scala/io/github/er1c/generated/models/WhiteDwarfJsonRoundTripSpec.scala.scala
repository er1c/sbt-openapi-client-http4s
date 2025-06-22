package io.github.er1c.generated.models

import io.circe.parser.*
import io.circe.syntax.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class WhiteDwarfJsonRoundTripSpec extends AnyFlatSpec with Matchers {
  "WhiteDwarf".should("round-trip JSON serialize/deserialize as subtype") in {
    val jsonStr = """{"carbonInfusion":"Ignition","type":"RedGiant"}"""
    val decoded = decode[WhiteDwarf](jsonStr)
    decoded.isRight shouldBe true
    val model = decoded.toOption.get
    val encoded = model.asJson.noSpaces
    val decodedAgain = decode[WhiteDwarf](encoded)
    decodedAgain.isRight shouldBe true
    decodedAgain.toOption.get shouldBe model
  }

  "WhiteDwarf as Star".should("round-trip JSON serialize/deserialize as parent type") in {
    val jsonStr = """{"carbonInfusion":"Ignition","type":"RedGiant"}"""
    val decoded = decode[Star](jsonStr)
    decoded.isRight shouldBe true
    val model = decoded.toOption.get
    val encoded = model.asJson.noSpaces
    val decodedAgain = decode[Star](encoded)
    decodedAgain.isRight shouldBe true
    decodedAgain.toOption.get shouldBe model
  }
}
