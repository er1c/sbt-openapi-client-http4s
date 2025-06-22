package io.github.er1c.generated.models

import io.circe.parser.*
import io.circe.syntax.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class StarTypeEnumJsonRoundTripSpec extends AnyFlatSpec with Matchers {
  "StarTypeEnum".should("round-trip JSON serialize/deserialize") in {
    val jsonStr = """"Supernova""""
    val decoded = decode[StarTypeEnum](jsonStr)
    decoded.isRight shouldBe true
    val model = decoded.toOption.get
    val encoded = model.asJson.noSpaces
    val decodedAgain = decode[StarTypeEnum](encoded)
    decodedAgain.isRight shouldBe true
    decodedAgain.toOption.get shouldBe model
  }
}
