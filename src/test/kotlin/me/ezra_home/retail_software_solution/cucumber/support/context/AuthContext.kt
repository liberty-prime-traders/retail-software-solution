package me.ezra_home.retail_software_solution.cucumber.support.context

import me.ezra_home.retail_software_solution.cucumber.support.TestConstants
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AuthContext(private val injectContext: InjectContext) {
  var authToken: String? = null
  var currentOrganizationId: UUID? = null
  var currentLocationId: UUID? = null

  fun initialize() {
    authToken = TestConstants.Tokens.PLATFORM_ADMIN
    currentOrganizationId = injectContext.find(InjectionKeys.ORGANIZATION)
    currentLocationId = injectContext.find(InjectionKeys.LOCATION)
  }
}
