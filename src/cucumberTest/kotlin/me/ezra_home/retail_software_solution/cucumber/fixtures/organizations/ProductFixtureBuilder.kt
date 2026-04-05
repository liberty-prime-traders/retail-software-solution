package me.ezra_home.retail_software_solution.cucumber.fixtures.organizations

import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.context.TransientKey
import me.ezra_home.retail_software_solution.cucumber.fixtures.FixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.ApiClient
import me.ezra_home.retail_software_solution.cucumber.support.DtoConverter
import me.ezra_home.retail_software_solution.organizations.business.product.public.OrganizationProductInsertDto
import org.springframework.stereotype.Component

@Component
class ProductFixtureBuilder(
  injectContext: InjectContext,
  apiClient: ApiClient,
  private val dtoConverter: DtoConverter,
) : FixtureBuilder<OrganizationProductInsertDto>(injectContext, apiClient) {

  override val endpoint = "/secured/products"

  override fun defaultDto() = OrganizationProductInsertDto(
    productName = "Test Product",
    productGroupId = injectContext.get(TransientKey.PRODUCT_GROUP),
    baseUnitId = injectContext.get(TransientKey.UNIT_VALUE),
  )

  override fun fromRow(row: Map<String, String>) =
    dtoConverter.fromRow(row, defaultDto(), OrganizationProductInsertDto::class.java)
}
