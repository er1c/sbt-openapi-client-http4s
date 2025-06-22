package io.github.er1c.generated.models

import io.circe.parser.*
import io.circe.syntax.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CategoryJsonRoundTripSpec extends AnyFlatSpec with Matchers {
  "Category".should("round-trip JSON serialize/deserialize") in {
    val jsonStr = """{"id":54,"name":"QDHdPbolzQIzvdajVQeyHnwnsrfTzzahwPcAgTDrdEwwtMpDHbnLUuPgthEGluQqkhTyLtAaQOqKsnjfkhuqBvOpG"}"""
    val decoded = decode[Category](jsonStr)
    decoded.isRight shouldBe true
    val model = decoded.toOption.get
    val encoded = model.asJson.noSpaces
    val decodedAgain = decode[Category](encoded)
    decodedAgain.isRight shouldBe true
    decodedAgain.toOption.get shouldBe model
  }
}
