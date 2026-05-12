package me.ezra_home.retail_software_solution.cucumber.fixtures.platform

import me.ezra_home.retail_software_solution.cucumber.fixtures.FixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.ApiClient
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.platform.business.authorization_pass.api.AuthorizationPassInsertDto
import me.ezra_home.retail_software_solution.platform.business.authorization_pass.api.PassType
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AuthorizationPassFixtureBuilder(
    injectContext: InjectContext,
    apiClient: ApiClient
) : FixtureBuilder<AuthorizationPassInsertDto>(injectContext, apiClient) {

    override val endpoint = "/secured/authorization-passes"

    override fun defaultDto() = AuthorizationPassInsertDto(
        passType = PassType.CREATE_ORGANIZATION,
        maxUseCount = 5,
        assignedToId = UUID.fromString("22222222-2222-2222-2222-222222222222")
    )
}
