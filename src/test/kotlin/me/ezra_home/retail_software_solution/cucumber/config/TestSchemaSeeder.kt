package me.ezra_home.retail_software_solution.cucumber.config

import jakarta.annotation.PostConstruct
import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.configuration.session.withSession
import me.ezra_home.retail_software_solution.cucumber.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.TestConstants
import me.ezra_home.retail_software_solution.organizations.business.location.LocationRepository
import me.ezra_home.retail_software_solution.organizations.business.location.LocationType
import me.ezra_home.retail_software_solution.organizations.model.LocationEntity
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationRepository
import me.ezra_home.retail_software_solution.platform.business.sysuser.SysUserService
import me.ezra_home.retail_software_solution.platform.model.OrganizationEntity
import me.ezra_home.retail_software_solution.util.business.SchemaCreator
import me.ezra_home.retail_software_solution.util.model.ReferenceNumberEntityListener
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Component
import java.util.UUID
import javax.sql.DataSource

@Component
@DependsOn(
  DataSourceBeanNames.PLATFORM_SCHEMA_LIQUIBASE,
  TestTableRegistrySetup.BEAN_NAME,
  ReferenceNumberEntityListener.BEAN_NAME
)
class TestSchemaSeeder(
  private val organizationRepository: OrganizationRepository,
  private val locationRepository: LocationRepository,
  private val sysUserService: SysUserService,
  private val injectContext: InjectContext,

  @param:Qualifier(DataSourceBeanNames.ORGANIZATION_SCHEMA_DATA_SOURCE)
  private val orgDataSource: DataSource,

  @param:Qualifier(DataSourceBeanNames.LOCATION_SCHEMA_DATA_SOURCE)
  private val locationDataSource: DataSource,

  @param:Value("\${spring.datasource.organization.changelog}")
  private val orgChangelog: String,

  @param:Value("\${spring.datasource.location.changelog}")
  private val locationChangelog: String
) {

  @PostConstruct
  fun initialize() {
    SchemaCreator.createSchema(TestConstants.Seed.ORG_SCHEMA, orgDataSource, orgChangelog)
    SchemaCreator.createSchema(TestConstants.Seed.LOCATION_SCHEMA, locationDataSource, locationChangelog)
    val userId = seedPlatformUser()
    seedOrganization(userId)
  }

  private fun seedPlatformUser(): UUID {
    return withSession(SessionContext(oktaId = TestConstants.Seed.PLATFORM_USER_OKTA_ID)) {
      val sysUser = sysUserService.addSystemUser()
      injectContext.persist(TestConstants.InjectionKeys.SYSTEM_USER_ID, sysUser.id)
      sysUser.id
    }
  }

  private fun seedOrganization(userId: UUID) {
    val organization = organizationRepository.findAll().firstOrNull { it.schemaName == TestConstants.Seed.ORG_SCHEMA }
      ?: withSession(SessionContext(systemUserId = userId)) {
        organizationRepository.save(
          OrganizationEntity(
            name = "Test Organization",
            subdomain = "test",
            schemaName = TestConstants.Seed.ORG_SCHEMA
          )
        )
      }
    injectContext.persist(TestConstants.InjectionKeys.ORGANIZATION, organization.getNullSafeId())
  }

  fun seedLocation() {
    val location = withSession(testSession()) {
      locationRepository.save(
        LocationEntity(
          locationType = LocationType.SHOP,
          name = "Test Location",
          schemaName = TestConstants.Seed.LOCATION_SCHEMA
        )
      )
    }
    SessionContextProvider.initLocation(location)
    injectContext.store(TestConstants.InjectionKeys.LOCATION, location.getNullSafeId())
  }

  private fun testSession(): SessionContext {
    val sysUserId = injectContext.get(TestConstants.InjectionKeys.SYSTEM_USER_ID)
    val orgId = injectContext.get(TestConstants.InjectionKeys.ORGANIZATION)
    return SessionContext(systemUserId = sysUserId).apply {
      organization = OrgSession(
        id = orgId,
        schemaName = TestConstants.Seed.ORG_SCHEMA,
        timezone = "UTC"
      )
      tenantFilterIsComplete = true
    }
  }
}
