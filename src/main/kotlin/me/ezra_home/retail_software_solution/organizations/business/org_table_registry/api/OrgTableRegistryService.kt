package me.ezra_home.retail_software_solution.organizations.business.org_table_registry.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.org_table_registry.OrgTableRegistryCache
import me.ezra_home.retail_software_solution.organizations.business.org_table_registry.OrgTableRegistryMapper
import me.ezra_home.retail_software_solution.platform.business.table_registry.api.TableRegistryService
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@TransactionalOnOrganizationSchema
class OrgTableRegistryService(
    private val orgTableRegistryCache: OrgTableRegistryCache,
    private val orgTableRegistryMapper: OrgTableRegistryMapper,
    private val tableRegistryService: TableRegistryService
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAll(): Collection<OrgTableRegistryResponseDto> =
        orgTableRegistryCache.getAllTables().map { orgTableRegistryMapper.toDto(it) }

    fun update(updateDto: OrgTableRegistryUpdateDto): OrgTableRegistryResponseDto {
        val allTables = orgTableRegistryCache.getAllTables()
        val existing = allTables.find { it.id == updateDto.id } ?: throw RtsGenericException("Org Table not found")
        val updated = updateDto.applyTo(existing)
        validateRequiredFields(updated)
        validateOrgWideUniqueness(updated.defaultPrefix, updated.displayName, updated.id, allTables)
        validatePlatformWideUniqueness(updated.defaultPrefix, updated.displayName, allTables)
        orgTableRegistryCache.upsertTable(updated)
        return orgTableRegistryMapper.toDto(updated)
    }

    private fun validateRequiredFields(dto: OrgTableRegistryDto) {
        StringUtils.requireHasValue(dto.defaultPrefix, "Default prefix is required")
        StringUtils.requireHasValue(dto.displayName, "Display name is required")
    }

    private fun validateOrgWideUniqueness(defaultPrefix: String, displayName: String, tableId: UUID?, allOrgTables: Collection<OrgTableRegistryDto>) {
        allOrgTables
            .find { StringUtils.isEquivalent(it.defaultPrefix, defaultPrefix) && it.id != tableId }
            ?.let { throw RtsGenericException("The default prefix '$defaultPrefix' conflicts with org table '${it.id}'") }

        allOrgTables
            .find { StringUtils.isEquivalent(it.displayName, displayName) && it.id != tableId }
            ?.let { throw RtsGenericException("An Org Table using the display name '$displayName' already exists") }
    }

    private fun validatePlatformWideUniqueness(defaultPrefix: String, displayName: String, allOrgTables: Collection<OrgTableRegistryDto>) {
        val overriddenRegistryIds = allOrgTables.map { it.registryId }.toSet()
        val platformTablesNotOverridden = tableRegistryService.getAllTableDtos()
            .filter { it.id !in overriddenRegistryIds }

        platformTablesNotOverridden
            .find { StringUtils.isEquivalent(it.defaultPrefix, defaultPrefix) }
            ?.let { throw RtsGenericException("The default prefix '$defaultPrefix' conflicts with platform table '${it.tableName}'") }

        platformTablesNotOverridden
            .find { StringUtils.isEquivalent(it.displayName, displayName) }
            ?.let { throw RtsGenericException("The display name '$displayName' conflicts with platform table '${it.tableName}'") }
    }
}
