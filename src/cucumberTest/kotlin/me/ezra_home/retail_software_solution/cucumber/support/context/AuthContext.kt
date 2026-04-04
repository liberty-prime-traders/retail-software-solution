package me.ezra_home.retail_software_solution.cucumber.support.context

import me.ezra_home.retail_software_solution.support.TestConstants
import org.springframework.stereotype.Component

@Component
class AuthContext {
  var authToken: String? = null

  fun initialize() {
    authToken = TestConstants.Tokens.PLATFORM_ADMIN
  }
}
