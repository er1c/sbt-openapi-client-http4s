// Discriminator value used: Supernova
package io.github.er1c.generated.models

import io.circe.parser.*
import io.circe.syntax.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SupernovaJsonRoundTripSpec extends AnyFlatSpec with Matchers {
  "Supernova".should("round-trip JSON serialize/deserialize as subtype") in {
    val jsonStr = """{"phase":"CoreCollapse","type":"Supernova"}"""
    val decoded = decode[Supernova](jsonStr)
    decoded.isRight shouldBe true
    val model = decoded.toOption.get
    val encoded = model.asJson.noSpaces
    val decodedAgain = decode[Supernova](encoded)
    decodedAgain.isRight shouldBe true
    decodedAgain.toOption.get shouldBe model
  }

  "Supernova as Star".should("round-trip JSON serialize/deserialize as parent type") in {
    val jsonStr = """{"phase":"CoreCollapse","type":"Supernova"}"""
    val decoded = decode[Star](jsonStr)
    decoded.isRight shouldBe true
    val model = decoded.toOption.get
    val encoded = model.asJson.noSpaces
    val decodedAgain = decode[Star](encoded)
    decodedAgain.isRight shouldBe true
    decodedAgain.toOption.get shouldBe model
  }
}
