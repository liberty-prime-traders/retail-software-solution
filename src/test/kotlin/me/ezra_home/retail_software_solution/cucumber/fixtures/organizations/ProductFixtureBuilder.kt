package me.ezra_home.retail_software_solution.cucumber.fixtures.organizations

import me.ezra_home.retail_software_solution.cucumber.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.context.InjectionKeys
import me.ezra_home.retail_software_solution.cucumber.fixtures.FixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.AuthenticatedRequestFactory
import me.ezra_home.retail_software_solution.cucumber.support.DtoConverter
import me.ezra_home.retail_software_solution.organizations.business.product.dto.OrganizationProductInsertDto
import org.springframework.stereotype.Component

@Component
class ProductFixtureBuilder(
  requestFactory: AuthenticatedRequestFactory,
  injectContext: InjectContext,
  private val dtoConverter: DtoConverter
) : FixtureBuilder<OrganizationProductInsertDto>(requestFactory, injectContext) {

  override val endpoint = "/secured/products"

  override fun defaultDto() = OrganizationProductInsertDto(
    productName = "Test Product",
    productGroupId = injectContext.get(InjectionKeys.PRODUCT_GROUP),
    baseUnitId = injectContext.get(InjectionKeys.UNIT_VALUE),
  )

  override fun fromRow(row: Map<String, String>) =
    dtoConverter.fromRow(row, defaultDto(), OrganizationProductInsertDto::class.java)
}
