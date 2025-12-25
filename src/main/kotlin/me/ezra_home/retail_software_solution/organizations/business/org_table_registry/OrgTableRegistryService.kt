package me.ezra_home.retail_software_solution.organizations.business.org_table_registry

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.org_table_registry.dto.OrgTableRegistryResponseDto
import me.ezra_home.retail_software_solution.organizations.business.org_table_registry.dto.OrgTableRegistryUpdateDto
import me.ezra_home.retail_software_solution.organizations.model.OrgTableRegistryEntity
import me.ezra_home.retail_software_solution.platform.business.table_registry.TableRegistryCache
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@TransactionalOnOrganizationSchema
class OrgTableRegistryService(
    private val orgTableRegistryCache: OrgTableRegistryCache,
    private val orgTableRegistryMapper: OrgTableRegistryMapper,
    private val platformTableRegistryCache: TableRegistryCache
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAll(): Collection<OrgTableRegistryResponseDto> =
        orgTableRegistryCache.getAllTables().map { orgTableRegistryMapper.toDto(it) }

    fun update(dto: OrgTableRegistryUpdateDto): OrgTableRegistryResponseDto {
        val allTables = orgTableRegistryCache.getAllTables()
        val entity = allTables.find { it.id == dto.id } ?: throw RtsGenericException("Org Table not found")
        orgTableRegistryMapper.patchEntity(dto, entity)
        validateRequiredFields(entity)
        validateOrgWideUniqueness(entity.defaultPrefix, entity.displayName, entity.id, allTables)
        validatePlatformWideUniqueness(entity.defaultPrefix, entity.displayName, allTables)
        orgTableRegistryCache.upsertTable(entity)
        return orgTableRegistryMapper.toDto(entity)
    }

    private fun validateRequiredFields(entity: OrgTableRegistryEntity) {
        StringUtils.getValueOrException(entity.defaultPrefix, "Default prefix is required")
        StringUtils.getValueOrException(entity.displayName, "Display name is required")
    }

    private fun validateOrgWideUniqueness(defaultPrefix: String, displayName: String, tableId: UUID?, allOrgTables: Collection<OrgTableRegistryEntity>) {
        allOrgTables
            .find { StringUtils.isEquivalent(it.defaultPrefix, defaultPrefix) && it.id != tableId }
            ?.let { throw RtsGenericException("The default prefix '$defaultPrefix' conflicts with org table '${it.id}'") }

        allOrgTables
            .find { StringUtils.isEquivalent(it.displayName, displayName) && it.id != tableId }
            ?.let { throw RtsGenericException("An Org Table using the display name '$displayName' already exists") }
    }

    private fun validatePlatformWideUniqueness(defaultPrefix: String, displayName: String, allOrgTables: Collection<OrgTableRegistryEntity>) {
        val overriddenRegistryIds = allOrgTables.map { it.registryId }.toSet()
        val platformTablesNotOverridden = platformTableRegistryCache.getAllTables()
            .filter { it.id !in overriddenRegistryIds }

        platformTablesNotOverridden
            .find { StringUtils.isEquivalent(it.defaultPrefix, defaultPrefix) }
            ?.let { throw RtsGenericException("The default prefix '$defaultPrefix' conflicts with platform table '${it.tableName}'") }

        platformTablesNotOverridden
            .find { StringUtils.isEquivalent(it.displayName, displayName) }
            ?.let { throw RtsGenericException("The display name '$displayName' conflicts with platform table '${it.tableName}'") }
    }
}
