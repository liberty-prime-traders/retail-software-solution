package me.ezra_home.retail_software_solution.cucumber.support.initialization

import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.cucumber.support.ApiClient
import me.ezra_home.retail_software_solution.cucumber.support.TestConstants
import me.ezra_home.retail_software_solution.cucumber.support.context.AuthContext
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.context.PersistentKey
import me.ezra_home.retail_software_solution.cucumber.support.context.ResponseContext
import me.ezra_home.retail_software_solution.organizations.business.location.LocationCache
import me.ezra_home.retail_software_solution.organizations.business.location.LocationType
import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationInsertDto
import me.ezra_home.retail_software_solution.platform.business.authorization_pass.PassType
import me.ezra_home.retail_software_solution.platform.business.authorization_pass.dto.AuthorizationPassInsertDto
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationCache
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationInsertDto
import me.ezra_home.retail_software_solution.util.model.ReferenceNumberEntityListener
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.DependsOn
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@DependsOn(
  DataSourceBeanNames.PLATFORM_SCHEMA_LIQUIBASE,
  TestTableRegistrySetup.BEAN_NAME,
  ReferenceNumberEntityListener.BEAN_NAME
)
class BoilerPlateDataInitializer(
  private val apiClient: ApiClient,
  private val authContext: AuthContext,
  private val injectContext: InjectContext,
  private val organizationCache: OrganizationCache,
  private val locationCache: LocationCache,
  private val holder: BoilerPlateSessionContextHolder
) {

  @EventListener(ApplicationReadyEvent::class)
  fun initialize() {
    authContext.authToken = TestConstants.Tokens.PLATFORM_ADMIN

    val platformUserId = createPlatformUser()
    val subdomain = reserveSubdomain()
    val passRecordId = issuePass(platformUserId)
    val orgId = createOrganization(subdomain, passRecordId)
    val locationId = createLocation()

    holder.org = organizationCache.getAllOrganizations()
      .firstOrNull { it.id == orgId }
      ?: error("Boilerplate org not found in cache after creation")

    holder.withOrgSession {
      holder.location = locationCache.getAllLocations()
        .firstOrNull { it.id == locationId }
        ?: error("Boilerplate location not found in cache after creation")
    }

    createOrgUser()
  }

  private fun createPlatformUser(): UUID {
    val response = apiClient.post("/secured/users")
    check(response.statusCode() == 200) { "Failed to create platform user: ${response.asString()}" }
    return ResponseContext.idFromResponse(response)
  }

  private fun createOrgUser() {
    authContext.authToken = TestConstants.Tokens.ORG_USER
    val response = apiClient.post("/secured/users")
    authContext.authToken = null
    check(response.statusCode() == 200) { "Failed to create org user: ${response.asString()}" }
  }

  private fun reserveSubdomain(): String {
    val response = apiClient.get("/secured/reserved-subdomains/verify?suggestedSubdomain=test")
    check(response.statusCode() == 200) { "Failed to reserve subdomain: ${response.asString()}" }
    return response.jsonPath().getString("subdomain")
  }

  private fun issuePass(userId: UUID): UUID {
    val response = apiClient.post(
      "/secured/authorization-passes",
      AuthorizationPassInsertDto(
        passType = PassType.CREATE_ORGANIZATION,
        maxUseCount = 1,
        assignedToId = userId
      )
    )
    check(response.statusCode() == 200) { "Failed to issue authorization pass: ${response.asString()}" }
    return ResponseContext.idFromResponse(response)
  }

  private fun createOrganization(subdomain: String, passRecordId: UUID): UUID {
    val secretCodeResponse = apiClient.get("/secured/authorization-passes/$passRecordId/secret-code")
    check(secretCodeResponse.statusCode() == 200) { "Failed to get secret code: ${secretCodeResponse.asString()}" }
    val secretCode = UUID.fromString(secretCodeResponse.jsonPath().getString("code"))

    val response = apiClient.post(
      "/secured/organizations",
      OrganizationInsertDto(name = "Test Organization", subdomain = subdomain, passCode = secretCode)
    )
    check(response.statusCode() == 200) { "Failed to create organization: ${response.asString()}" }
    val orgId = ResponseContext.idFromResponse(response)
    injectContext.persist(PersistentKey.ORGANIZATION, orgId)
    return orgId
  }

  private fun createLocation(): UUID {
    val response = apiClient.post(
      "/secured/locations",
      LocationInsertDto(locationType = LocationType.SHOP, name = "Test Location")
    )
    check(response.statusCode() == 200) { "Failed to create location: ${response.asString()}" }
    return ResponseContext.idFromResponse(response)
  }
}
