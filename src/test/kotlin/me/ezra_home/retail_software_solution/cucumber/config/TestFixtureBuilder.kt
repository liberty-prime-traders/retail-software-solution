package me.ezra_home.retail_software_solution.cucumber.config

import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.product_category.ProductCategoryRepository
import me.ezra_home.retail_software_solution.organizations.business.product_group.ProductGroupRepository
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.UnitGroupRepository
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitValueRepository
import me.ezra_home.retail_software_solution.organizations.model.ProductCategoryEntity
import me.ezra_home.retail_software_solution.organizations.model.ProductGroupEntity
import me.ezra_home.retail_software_solution.organizations.model.UnitGroupEntity
import me.ezra_home.retail_software_solution.organizations.model.UnitValueEntity
import org.springframework.stereotype.Component
import java.util.UUID

data class ProductFixture(
  val productGroupId: UUID,
  val baseUnitId: UUID
)

@Component
class TestFixtureBuilder(
  private val productCategoryRepository: ProductCategoryRepository,
  private val productGroupRepository: ProductGroupRepository,
  private val unitGroupRepository: UnitGroupRepository,
  private val unitValueRepository: UnitValueRepository
) {

  fun createProductFixture(seed: String = UUID.randomUUID().toString().substring(0, 8)): ProductFixture {
    return withTestSession {
      val category = productCategoryRepository.save(
        ProductCategoryEntity(
          categoryName = "Category-$seed",
          description = "Fixture category $seed"
        )
      )

      val productGroup = productGroupRepository.save(
        ProductGroupEntity(
          groupName = "Group-$seed",
          description = "Fixture group $seed",
          categoryId = category.id!!
        )
      )

      val unitGroup = unitGroupRepository.save(
        UnitGroupEntity(
          name = "Units-$seed",
          description = "Fixture unit group $seed"
        )
      )

      val baseUnit = unitValueRepository.save(
        UnitValueEntity(
          name = "Piece-$seed",
          description = "Fixture base unit $seed",
          code = "PC$seed",
          unitGroupId = unitGroup.id!!,
          baseUnit = null,
          conversionFactor = null
        )
      )

      ProductFixture(
        productGroupId = productGroup.id!!,
        baseUnitId = baseUnit.id!!
      )
    }
  }

  private fun <T> withTestSession(block: () -> T): T {
    val session = SessionContext().apply {
      systemUserId = UUID.fromString("00000000-0000-0000-0000-000000000001")
      oktaId = "test-okta-id"
      organizationId = UUID.fromString("00000000-0000-0000-0000-000000000001")
      organizationSchemaName = "public"
      locationId = UUID.fromString("00000000-0000-0000-0000-000000000001")
      locationSchemaName = "public"
      tenantFilterIsComplete = true
    }

    SessionContextProvider.setSession(session)
    try {
      return block()
    } finally {
      SessionContextProvider.clear()
    }
  }
}
