package io.github.er1c.generated.models

import io.circe.parser.*
import io.circe.syntax.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TagJsonRoundTripSpec extends AnyFlatSpec with Matchers {
  "Tag".should("round-trip JSON serialize/deserialize") in {
    val jsonStr = """{"id":87,"name":"ueMpImhToFAmFcsZSbrCVBVMLHJMwXuVSPXPIFRJKMQFibLUhRYOQHrMaSkyRKCaOpsDVrHdwdQJpII"}"""
    val decoded = decode[Tag](jsonStr)
    decoded.isRight shouldBe true
    val model = decoded.toOption.get
    val encoded = model.asJson.noSpaces
    val decodedAgain = decode[Tag](encoded)
    decodedAgain.isRight shouldBe true
    decodedAgain.toOption.get shouldBe model
  }
}
