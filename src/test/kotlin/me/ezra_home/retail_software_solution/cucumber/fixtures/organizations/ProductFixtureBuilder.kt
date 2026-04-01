package me.ezra_home.retail_software_solution.cucumber.fixtures.organizations

import me.ezra_home.retail_software_solution.cucumber.context.organizations.ProductGroupContext
import me.ezra_home.retail_software_solution.cucumber.context.organizations.UnitContext
import me.ezra_home.retail_software_solution.cucumber.fixtures.FixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.AuthenticatedRequestFactory
import me.ezra_home.retail_software_solution.cucumber.support.DtoConverter
import me.ezra_home.retail_software_solution.cucumber.support.InjectContext
import me.ezra_home.retail_software_solution.organizations.business.product.dto.OrganizationProductInsertDto
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ProductFixtureBuilder(
  requestFactory: AuthenticatedRequestFactory,
  injectContext: InjectContext,
  private val dtoConverter: DtoConverter
) : FixtureBuilder<OrganizationProductInsertDto>(requestFactory, injectContext) {

  override val endpoint = "/secured/products"

  override fun defaultDto() = OrganizationProductInsertDto(
    productName = "Test Product",
    productGroupId = UUID.fromString(injectContext.get(ProductGroupContext.ID)),
    baseUnitId = UUID.fromString(injectContext.get(UnitContext.VALUE_ID)),
  )

  override fun fromRow(row: Map<String, String>) =
    dtoConverter.fromRow(row, defaultDto(), OrganizationProductInsertDto::class.java)
}
