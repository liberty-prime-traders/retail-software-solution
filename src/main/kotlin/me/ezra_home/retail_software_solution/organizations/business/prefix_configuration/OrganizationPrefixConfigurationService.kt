package me.ezra_home.retail_software_solution.organizations.business.prefix_configuration

import me.ezra_home.retail_software_solution.business.prefix_configuration.shared.dto.PrefixConfigurationResponseDto
import me.ezra_home.retail_software_solution.business.prefix_configuration.shared.dto.PrefixConfigurationUpdateDto
import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.model.OrganizationPrefixConfigurationEntity
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationCache
import me.ezra_home.retail_software_solution.platform.business.table_registry.TableRegistryService
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class OrganizationPrefixConfigurationService(
    private val organizationPrefixConfigurationCache: OrganizationPrefixConfigurationCache,
    private val organizationPrefixConfigurationMapper: OrganizationPrefixConfigurationMapper,
    private val organizationCache: OrganizationCache,
    private val tableRegistryService: TableRegistryService,
) {
    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getForTableRegistry(tableRegistryId: UUID): Collection<PrefixConfigurationResponseDto> =
        organizationPrefixConfigurationCache.getForTableRegistry(tableRegistryId)
            .map { organizationPrefixConfigurationMapper.toDto(it) }

    fun updatePrefix(dto: PrefixConfigurationUpdateDto): PrefixConfigurationResponseDto {
        val organizationId = SessionContextProvider.getOrganizationId()
        val organization = organizationCache.getAllOrganizations().find { it.id == organizationId }
            ?: throw RtsGenericException("Organization not found")
        SessionContextProvider.initOrganization(organization)
        val entity = organizationPrefixConfigurationCache.getById(dto.prefixConfigurationId)
            ?: throw RtsGenericException("Prefix configuration not found")
        entity.prefix = dto.prefix
        entity.updatedOn = OffsetDateTime.now()
        return organizationPrefixConfigurationMapper.toDto(
            organizationPrefixConfigurationCache.upsertPrefixConfiguration(
                entity
            )
        )
    }

    fun bulkCreateForRegistry(tableRegistryId: UUID, defaultPrefix: String, userId: UUID) {
        val entity = OrganizationPrefixConfigurationEntity(
            tableRegistryId = tableRegistryId,
            prefix = defaultPrefix
        ).apply {
            createdById = userId
            createdOn = OffsetDateTime.now()
        }
        organizationPrefixConfigurationCache.upsertPrefixConfiguration(entity)
    }

    fun getPrefixForTableName(tableName: String): String? {
        val tableRegistry = tableRegistryService.getTableRegistryForTableName(tableName)
        val prefixConfig = getPrefixConfigurationForTableRegistry(tableRegistry.id!!)
        return prefixConfig?.prefix ?: tableRegistry.defaultPrefix
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getPrefixConfigurationForTableRegistry(tableRegistryId: UUID): OrganizationPrefixConfigurationEntity? {
        return organizationPrefixConfigurationCache.getForTableRegistry(tableRegistryId)
            .firstOrNull()
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getPrefixConfigurationForTable(tableName: String): OrganizationPrefixConfigurationEntity? {
        val tableRegistry = tableRegistryService.getTableRegistryForTableName(tableName)
        return getPrefixConfigurationForTableRegistry(tableRegistry.id!!)
    }
}
