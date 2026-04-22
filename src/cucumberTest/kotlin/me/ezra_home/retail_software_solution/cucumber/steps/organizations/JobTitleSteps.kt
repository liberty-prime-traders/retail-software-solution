package me.ezra_home.retail_software_solution.cucumber.steps.organizations

import io.cucumber.java.en.Given
import me.ezra_home.retail_software_solution.cucumber.fixtures.organizations.JobTitleFixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.context.TransientKey

class JobTitleSteps(
  private val jobTitleFixtureBuilder: JobTitleFixtureBuilder,
  private val injectContext: InjectContext
) {

  @Given("a job title exists")
  fun createJobTitle() {
    injectContext.store(TransientKey.JOB_TITLE, jobTitleFixtureBuilder.create())
  }
}
