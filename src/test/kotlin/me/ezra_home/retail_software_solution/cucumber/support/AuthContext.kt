package me.ezra_home.retail_software_solution.cucumber.support

import java.util.UUID

object AuthContext {

  var authToken: String? = null
  var currentOrganizationId: UUID? = null
  var currentLocationId: UUID? = null

  fun reset() {
    authToken = null
    currentOrganizationId = null
    currentLocationId = null
  }
}
