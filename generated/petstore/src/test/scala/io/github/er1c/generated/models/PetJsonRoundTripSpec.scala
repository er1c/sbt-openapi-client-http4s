package io.github.er1c.generated.models

import io.circe.parser.*
import io.circe.syntax.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PetJsonRoundTripSpec extends AnyFlatSpec with Matchers {
  "Pet".should("round-trip JSON serialize/deserialize") in {
    val jsonStr = """{"name":"uJYIMwuLnaVdqnzkokbuNinlPMZCCEnaTTmvvddtdypXcFQtKkLECqUzLFNgFqesTIp","tags":[{"id":36,"name":"FnwPkFYkAhlarfkuqgfFLQwYkRknXupmdFAvHeTkwqQydVxqSItQvGlBHQQuSh"},{"id":50,"name":"doSfgASRBndTzDQNbCmZgxGRwNonnaWgZLYXLCXnKMMvlDORzXbAOnqFAJqGFsAhqMRaQQKtknpkRdCCVAFFzorGZVWOTtQ"}],"photoUrls":["RZFQfXpVwcICTLzbeOjIzSlunPBJuEGdbc","kzPzgOyxiFBdstYAlEmnrzZnNdUueZgrgYZtgLmVZEvNzPnvfvfUEzIOFDVwzEewNcFqhRDcHMFSzEjYdiAmhOZxn"],"id":97,"status":"available","category":{"id":43,"name":"sVUSCsZHoESsaTyMJigWBSPdfBjLlmEWTslNduBSrCRCtJMd"}}"""
    val decoded = decode[Pet](jsonStr)
    decoded.isRight shouldBe true
    val model = decoded.toOption.get
    val encoded = model.asJson.noSpaces
    val decodedAgain = decode[Pet](encoded)
    decodedAgain.isRight shouldBe true
    decodedAgain.toOption.get shouldBe model
  }
}
