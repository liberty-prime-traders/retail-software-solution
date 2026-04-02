package me.ezra_home.retail_software_solution.cucumber.context

import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AuthContext {
  var authToken: String? = null
  var currentUserId: UUID? = null
  var currentOrganizationId: UUID? = null
  var currentLocationId: UUID? = null

  fun reset() {
    authToken = null
    currentUserId = null
    currentOrganizationId = null
    currentLocationId = null
  }
}
