package io.github.er1c.generated.models

import io.circe.parser.*
import io.circe.syntax.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class UserJsonRoundTripSpec extends AnyFlatSpec with Matchers {
  "User".should("round-trip JSON serialize/deserialize") in {
    val jsonStr = """{"email":"tbVdQjSQbFzwNTaFPAQxVaLlnnFfCGJHAqvivukFCHjfoskfQBjSxEPF","username":"sImwdUOQdqIdVzMEbZIZfxsgibjIFEfLCBmonVKfplJyNbNpoEAaOQuqLfimiCPEGDbaRNsYyGZKjJBZ","userStatus":89,"lastName":"ANuXdwYEYdfErVqDbqIpabQuUSsRyNkYzfWoB","firstName":"brioGwcgJxZXdxwmTLCBDsMZFrcbeSRWtfLoNik","id":89,"phone":"dVMNkaBXKTYrHUIpKgVpFdeFPktkebsipkygFDPStrmOCsZYHkgMzNIECpsdtVEsXjWF","password":"FTzmIGNPkMsDLoYqhtMOAATjayvXVo"}"""
    val decoded = decode[User](jsonStr)
    decoded.isRight shouldBe true
    val model = decoded.toOption.get
    val encoded = model.asJson.noSpaces
    val decodedAgain = decode[User](encoded)
    decodedAgain.isRight shouldBe true
    decodedAgain.toOption.get shouldBe model
  }
}
