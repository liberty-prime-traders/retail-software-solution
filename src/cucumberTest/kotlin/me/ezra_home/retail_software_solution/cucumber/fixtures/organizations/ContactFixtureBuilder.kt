package me.ezra_home.retail_software_solution.cucumber.fixtures.organizations

import me.ezra_home.retail_software_solution.cucumber.fixtures.FixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.ApiClient
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactInsertDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactType
import me.ezra_home.retail_software_solution.organizations.business.contact.api.IdentityType
import org.springframework.stereotype.Component

@Component
class ContactFixtureBuilder(
    injectContext: InjectContext,
    apiClient: ApiClient
) : FixtureBuilder<ContactInsertDto>(injectContext, apiClient) {

    override val endpoint = "/secured/contacts"

    override fun defaultDto() = ContactInsertDto(
        contactType = ContactType.CUSTOMER,
        identityType = IdentityType.INDIVIDUAL,
        firstName = "John",
        lastName = "Doe",
        email = "john.doe@example.com",
        phone = "1234567890",
        address = "123 Test St"
    )
}
