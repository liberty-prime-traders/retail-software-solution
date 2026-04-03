package me.ezra_home.retail_software_solution.cucumber.support.initialization

import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.cucumber.support.ApiClient
import me.ezra_home.retail_software_solution.cucumber.support.TestConstants
import me.ezra_home.retail_software_solution.cucumber.support.context.AuthContext
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectionKeys
import me.ezra_home.retail_software_solution.cucumber.support.context.ResponseContext
import me.ezra_home.retail_software_solution.organizations.business.location.LocationType
import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationInsertDto
import me.ezra_home.retail_software_solution.platform.business.authorization_pass.AuthorizationPassRepository
import me.ezra_home.retail_software_solution.platform.business.authorization_pass.AuthorizationPassService
import me.ezra_home.retail_software_solution.platform.business.authorization_pass.PassType
import me.ezra_home.retail_software_solution.platform.business.authorization_pass.dto.AuthorizationPassInsertDto
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationInsertDto
import me.ezra_home.retail_software_solution.util.model.ReferenceNumberEntityListener
import org.springframework.context.annotation.DependsOn
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
  private val authorizationPassService: AuthorizationPassService
) {

  @EventListener(ApplicationReadyEvent::class)
  fun initialize() {
    authContext.authToken = TestConstants.Tokens.PLATFORM_ADMIN

    val platformUserId = createUser()
    val subdomain = reserveSubdomain()
    val passRecordId = issuePass(platformUserId)
    val orgId = createOrganization(subdomain, passRecordId)

    authContext.currentOrganizationId = orgId
    createLocation()

    authContext.authToken = TestConstants.Tokens.ORG_USER
    createUser()

    authContext.authToken = null
    authContext.currentOrganizationId = null
  }

  private fun createUser(): UUID {
    val response = apiClient.post("/secured/users")
    check(response.statusCode() == 200) { "Failed to create user: ${response.asString()}" }
    val userId = ResponseContext.idFromResponse(response)
    injectContext.persist(InjectionKeys.SYSTEM_USER_ID, userId)
    return userId
  }

  private fun reserveSubdomain(): String {
    val suggestedSubdomain = "test"
    val response = apiClient.get("/secured/reserved-subdomains/verify?suggestedSubdomain=$suggestedSubdomain")
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
    val secretCode = authorizationPassService.getSecretCode(passRecordId)
    val response = apiClient.post(
      "/secured/organizations",
      OrganizationInsertDto(name = "Test Organization", subdomain = subdomain, passCode = secretCode)
    )
    check(response.statusCode() == 200) { "Failed to create organization: ${response.asString()}" }
    val orgId = ResponseContext.idFromResponse(response)
    injectContext.persist(InjectionKeys.ORGANIZATION, orgId)
    return orgId
  }

  private fun createLocation() {
    val response = apiClient.post(
      "/secured/locations",
      LocationInsertDto(locationType = LocationType.SHOP, name = "Test Location")
    )
    check(response.statusCode() == 200) { "Failed to create location: ${response.asString()}" }
    injectContext.persist(InjectionKeys.LOCATION, ResponseContext.idFromResponse(response))
  }
}
