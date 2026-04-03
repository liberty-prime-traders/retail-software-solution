package me.ezra_home.retail_software_solution.cucumber.support.initialization

import jakarta.annotation.PostConstruct
import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.cucumber.support.TestConstants
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectionKeys
import me.ezra_home.retail_software_solution.organizations.business.location.LocationService
import me.ezra_home.retail_software_solution.organizations.business.location.LocationType
import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationInsertDto
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationService
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationInsertDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.SysUserService
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
class TestSchemaSeeder(
    private val organizationService: OrganizationService,
    private val locationService: LocationService,
    private val sysUserService: SysUserService,
    private val injectContext: InjectContext,
) {

  @PostConstruct
  fun initialize() {
      seedUser(TestConstants.Okta.PLATFORM_USER)
      seedOrganization()
      seedLocation()
      seedUser(TestConstants.Okta.ORGANIZATION_USER)
      SessionContextProvider.clear()
  }

  private fun seedUser(oktaId: String) {
      SessionContextProvider.getSession().oktaId = oktaId
      val sysUser = sysUserService.addSystemUser()
      injectContext.persist(InjectionKeys.SYSTEM_USER_ID, sysUser.id)
      SessionContextProvider.initSystemUser(sysUser.id)
  }

  private fun seedOrganization() {
    val organization = organizationService.createOrganization(
        OrganizationInsertDto(
            name = "Test Organization",
            subdomain = "test",
            passCode = UUID.randomUUID()
        )
    )
    injectContext.persist(InjectionKeys.ORGANIZATION, organization.id)
  }

  private fun seedLocation() {
    val location = locationService.createLocation(
        LocationInsertDto(
            locationType = LocationType.SHOP,
            name = "Test Location"
        )
    )
    injectContext.persist(InjectionKeys.LOCATION, location.id)
  }
}
