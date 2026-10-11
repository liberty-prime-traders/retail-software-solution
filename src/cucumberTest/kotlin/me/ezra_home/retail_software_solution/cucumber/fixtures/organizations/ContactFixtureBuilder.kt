package me.ezra_home.retail_software_solution.cucumber.fixtures.organizations

import me.ezra_home.retail_software_solution.cucumber.fixtures.FixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.ApiClient
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactInsertDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactType
import me.ezra_home.retail_software_solution.organizations.business.contact.api.IdentityType
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ContactFixtureBuilder(
  injectContext: InjectContext,
  apiClient: ApiClient
) : FixtureBuilder<ContactInsertDto>(injectContext, apiClient) {

  override val endpoint = "/secured/contacts"

  override fun defaultDto() = ContactInsertDto(
    contactTypes = setOf(ContactType.CUSTOMER),
    identityType = IdentityType.ORGANIZATION,
    companyName = "Test Contact ${UUID.randomUUID()}"
  )
}
