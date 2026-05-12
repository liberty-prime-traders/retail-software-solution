package me.ezra_home.retail_software_solution.cucumber.fixtures.organizations

import me.ezra_home.retail_software_solution.cucumber.fixtures.FixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.ApiClient
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.organizations.business.jobtitle.api.JobTitleInsertDto
import org.springframework.stereotype.Component

@Component
class JobTitleFixtureBuilder(
  injectContext: InjectContext,
  apiClient: ApiClient
) : FixtureBuilder<JobTitleInsertDto>(injectContext, apiClient) {

  override val endpoint = "/secured/job-title"

  override fun defaultDto() = JobTitleInsertDto(value = "Test Job Title")
}
