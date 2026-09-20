package me.ezra_home.retail_software_solution.cucumber.support.initialization

import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.cucumber.support.ApiClient
import me.ezra_home.retail_software_solution.cucumber.support.TestUserRegistry
import me.ezra_home.retail_software_solution.cucumber.support.context.AuthContext
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.context.PersistentKey
import me.ezra_home.retail_software_solution.cucumber.support.context.ResponseContext
import me.ezra_home.retail_software_solution.organizations.business.location.LocationCache
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationInsertDto
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationType
import me.ezra_home.retail_software_solution.platform.business.authorization_pass.api.AuthorizationPassInsertDto
import me.ezra_home.retail_software_solution.platform.business.authorization_pass.api.PassType
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationCache
import me.ezra_home.retail_software_solution.platform.business.organization.api.OrganizationInsertDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.SysUserService
import me.ezra_home.retail_software_solution.platform.business.table_registry.TableRegistryCache
import me.ezra_home.retail_software_solution.support.TestConstants
import me.ezra_home.retail_software_solution.util.model.ReferenceNumberEntityListener
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.DependsOn
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@DependsOn(
  DataSourceBeanNames.PLATFORM_SCHEMA_LIQUIBASE,
  ReferenceNumberEntityListener.BEAN_NAME
)
class BoilerPlateDataInitializer(
  private val apiClient: ApiClient,
  private val authContext: AuthContext,
  private val injectContext: InjectContext,
  private val organizationCache: OrganizationCache,
  private val locationCache: LocationCache,
  private val tableRegistryCache: TableRegistryCache,
  private val sysUserService: SysUserService
) {

  companion object {
    const val SUBDOMAIN = "test"
  }

  @EventListener(ApplicationReadyEvent::class)
  fun initialize() {
    val existingOrganization = organizationCache.getAllOrganizations()
    if (existingOrganization.isEmpty()) {
      validateAllTables()
      authContext.authToken = TestConstants.Tokens.PLATFORM_ADMIN
      val platformUserId = createPlatformUser()
      val passRecordId = issuePass(platformUserId)
      reserveSubdomain()
      createOrganization(passRecordId)
      createLocation()
      createOrgUser()

    } else {
      existingOrganization.find { it.subdomain == SUBDOMAIN }?.let { organization ->
        injectContext.persist(PersistentKey.ORGANIZATION, organization.id)
        SessionContextProvider.initOrganization(organization)
        locationCache.getAllLocations().firstOrNull()?.let { location ->
          injectContext.persist(PersistentKey.LOCATION, location.id)
        }
        SessionContextProvider.clear()
      }
    }
  }

  private fun createPlatformUser(): UUID {
    val platformUserId = sysUserService.createUser(
      email = "platform-admin@test.local",
      localFirstName = "Fake",
      localLastName = "Platform Admin"
    )
    TestUserRegistry.platformAdminUserId = platformUserId
    return platformUserId
  }

  private fun createOrgUser() {
    val orgUserId = sysUserService.createUser(
      email = "org-user@test.local",
      localFirstName = "Fake",
      localLastName = "Organization User"
    )
    TestUserRegistry.organizationUserId = orgUserId
  }

  private fun reserveSubdomain() {
    val response = apiClient.get("/secured/reserved-subdomains/verify?suggestedSubdomain=$SUBDOMAIN")
    check(response.statusCode() == 200) { "Failed to reserve subdomain: ${response.asString()}" }
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

  private fun createOrganization(passRecordId: UUID) {
    val secretCodeResponse = apiClient.get("/secured/authorization-passes/$passRecordId/secret-code")
    check(secretCodeResponse.statusCode() == 200) { "Failed to get secret code: ${secretCodeResponse.asString()}" }
    val secretCode = UUID.fromString(secretCodeResponse.jsonPath().getString("code"))

    val response = apiClient.post(
      "/secured/organizations",
      OrganizationInsertDto(name = "Test Organization", subdomain = SUBDOMAIN, passCode = secretCode)
    )
    check(response.statusCode() == 200) { "Failed to create organization: ${response.asString()}" }
    injectContext.persist(PersistentKey.ORGANIZATION, ResponseContext.idFromResponse(response))
  }

  private fun createLocation() {
    val response = apiClient.post(
      "/secured/locations",
      LocationInsertDto(locationType = LocationType.SHOP, name = "Test Location")
    )
    check(response.statusCode() == 200) { "Failed to create location: ${response.asString()}" }
    injectContext.persist(PersistentKey.LOCATION, ResponseContext.idFromResponse(response))
  }

  private fun validateAllTables() {
    tableRegistryCache.getAllTables()
      .filter { !it.validated }
      .forEach {
        it.copy(validated = true).also { validated ->
          tableRegistryCache.save(validated)
        }
      }
  }
}
