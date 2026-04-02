package me.ezra_home.retail_software_solution.cucumber.fixtures.platform

import me.ezra_home.retail_software_solution.cucumber.support.AuthenticatedRequestFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OrgAdminFixtureBuilder(private val requestFactory: AuthenticatedRequestFactory) {

  fun register(userId: UUID) {
    val response = requestFactory.jsonRequest().post("/secured/organization-admins/$userId")
    check(response.statusCode == 200) {
      "Failed to register org admin for user $userId: ${response.statusCode} ${response.asString()}"
    }
  }
}
