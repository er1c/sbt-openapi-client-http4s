package io.github.er1c.generated.models

import io.circe.parser.*
import io.circe.syntax.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PetJsonRoundTripSpec extends AnyFlatSpec with Matchers {
  "Pet".should("round-trip JSON serialize/deserialize") in {
    val jsonStr = """{"name":"sCStDcGoykctwhfZrpyXXepDNweYZMNkuKmVOjCcpQarfzHsHIM","tags":[{"id":2,"name":"JUAKBDpQNxjGAkMjkwoMgzPNAtqHEKRbRwKYqnhprXHBEfMJbCkOHvbmZdOwlk"},{"id":11,"name":"RuUgVhZrFweEivmbQbThqSGktMBfeMJwamcklGDxAdWoEKfNlWrMcRtkbIdqCNXEYbtYJw"}],"photoUrls":["yTVAZYghXpJGecBVoFeumfevNBCM","VzLNwPjCPZxcNJpuUIiwcpyWvVepXrGwVwLFvvXbXcgge"],"id":37,"status":"pending","category":{"id":5,"name":"HpsHhiihjjuiCIvYGjaRruDtCBvCrgNauTVouueaNTfyYDNsymeFcHTsUCZpH"}}"""
    val decoded = decode[Pet](jsonStr)
    decoded.isRight shouldBe true
    val model = decoded.toOption.get
    val encoded = model.asJson.noSpaces
    val decodedAgain = decode[Pet](encoded)
    decodedAgain.isRight shouldBe true
    decodedAgain.toOption.get shouldBe model
  }
}
