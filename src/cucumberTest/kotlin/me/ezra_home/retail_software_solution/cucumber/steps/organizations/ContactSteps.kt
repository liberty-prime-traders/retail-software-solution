package me.ezra_home.retail_software_solution.cucumber.steps.organizations

import io.cucumber.java.en.Given
import me.ezra_home.retail_software_solution.cucumber.fixtures.organizations.ContactFixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.context.TransientKey

class ContactSteps(
    private val contactFixtureBuilder: ContactFixtureBuilder,
    private val injectContext: InjectContext
) {

    @Given("a contact exists")
    fun createContact() {
        injectContext.store(TransientKey.CONTACT, contactFixtureBuilder.create())
    }
}
