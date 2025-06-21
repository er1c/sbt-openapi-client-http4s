package io.github.er1c.openapi.parser.model

import io.github.er1c.openapi.generator.ModelGenerator
import io.github.er1c.openapi.parser.OpenApiParser
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CosmologySchemaSpec extends AnyFlatSpec with Matchers {

  "ModelGenerator" should "generate a single file for Galaxy ADT with inlined children" in {
    val specPath = getClass.getResource("/specs/cosmology.yml").getPath
    val openApi = OpenApiParser.parseFromFile(specPath)
    val files = ModelGenerator.generateModels(openApi, "io.github.er1c.test.cosmology")

    // Check that the parent ADT file exists
    files.keySet should contain("Galaxy")

    // Check that the children are NOT generated as separate files
    files.keySet should not contain "ElipticalGalaxy"
    files.keySet should not contain "SpiralGalaxy"

    // Check the content of the generated Galaxy.scala file
    val galaxyCode = files("Galaxy")

    // Assert that the sealed trait is there
    galaxyCode should include("sealed trait Galaxy derives ConfiguredCodec")

    // Assert that ElipticalGalaxy is defined inside
    galaxyCode should include("final case class ElipticalGalaxy")
    galaxyCode should include("eccentricity: ElipticalGalaxyEccentricity")
    galaxyCode should include("extends Galaxy")

    // Assert that SpiralGalaxy is defined inside
    galaxyCode should include("final case class SpiralGalaxy")
    galaxyCode should include("armcount: SpiralGalaxyArmcount")
    galaxyCode should include("extends Galaxy")

    // Assert that the companion object has the correct codecs
    galaxyCode should include("object Galaxy")
    galaxyCode should include("given Decoder[Galaxy] = Decoder[ElipticalGalaxy].widen or Decoder[SpiralGalaxy].widen")
    galaxyCode should include("given Encoder[Galaxy] = Encoder.instance")
  }

  it should "generate correct code for Star schema with discriminator" in {
    val specPath = getClass.getResource("/specs/cosmology.yml").getPath
    val openApi = OpenApiParser.parseFromFile(specPath)
    val files = ModelGenerator.generateModels(openApi, "io.github.er1c.test.cosmology")

    val starExpected = """package io.github.er1c.test.cosmology.models

import io.circe.syntax._
import io.circe.{Decoder, Encoder}

sealed trait Star {
  def `type`: String // The discriminator property
}

object Star {
  implicit val decoder: Decoder[Star] = Decoder.instance { c =>
    c.downField("type").as[String].flatMap {
      case "RedGiant" => c.as[RedGiant]
      case "Supernova" => c.as[Supernova]
      case "WhiteDwarf" => c.as[WhiteDwarf]
      case other => Left(io.circe.DecodingFailure(s"Unknown value '$other' for discriminator 'type' in Star", c.history))
    }
  }

  implicit val encoder: Encoder[Star] = Encoder.instance {
    case t: RedGiant => t.asJson
    case t: Supernova => t.asJson
    case t: WhiteDwarf => t.asJson
  }
}
"""
    files.keySet should contain ("Star")
    files("Star").trim shouldBe starExpected.trim
  }
}
