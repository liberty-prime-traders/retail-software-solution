package me.ezra_home.retail_software_solution.cucumber.fixtures.organizations

import org.springframework.stereotype.Component
import java.util.UUID

data class ProductFixture(
  val productGroupId: UUID,
  val baseUnitId: UUID
)

@Component
class ProductFixtureBuilder(
  private val productGroupFixtureBuilder: ProductGroupFixtureBuilder,
  private val unitFixtureBuilder: UnitFixtureBuilder
) {

  fun create(): ProductFixture {
    val productGroupId = productGroupFixtureBuilder.create()
    val baseUnitId = unitFixtureBuilder.createBaseUnit()
    return ProductFixture(productGroupId = productGroupId, baseUnitId = baseUnitId)
  }
}
