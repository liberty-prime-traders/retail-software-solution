package me.ezra_home.retail_software_solution.platform.business.table_registry.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.table_registry.TableRegistryCache
import me.ezra_home.retail_software_solution.platform.business.table_registry.TableRegistryMapper
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.model.TableName
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@TransactionalOnPlatformSchema
class TableRegistryService(
    private val tableRegistryCache: TableRegistryCache,
    private val tableRegistryMapper: TableRegistryMapper
) {

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getAllTableDtos(): Collection<TableRegistryDto> = tableRegistryCache.getAllTables()

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getAll(): Collection<TableRegistryResponseDto> =
        tableRegistryCache.getAllTables().map { tableRegistryMapper.toDto(it) }

    fun validate(id: UUID): TableRegistryResponseDto {
        val allTables = tableRegistryCache.getAllTables()
        val dto = allTables.find { it.id == id } ?: throw RtsGenericException("Table not found")
        if (!dto.validated) {
            validateName(dto.tableName)
            val updatedRegistry = tableRegistryCache.save(dto.copy(validated = true))
            return tableRegistryMapper.toDto(updatedRegistry)
        }
        return tableRegistryMapper.toDto(dto)
    }

    fun validateAll(): Collection<TableRegistryResponseDto> {
        val allTables = tableRegistryCache.getAllTables()
        val unvalidatedTables = allTables.filter { !it.validated }
        unvalidatedTables.forEach { validateName(it.tableName) }
        val savedTables = tableRegistryCache.saveAll(unvalidatedTables.map { it.copy(validated = true) })
            .associateBy { it.id }
        return allTables.map { tableRegistryMapper.toDto(savedTables[it.id] ?: it) }
    }

    private fun validateName(name: String?) {
        require(TableName.exists(name)) { "Table name '$name' is not recognized in the system" }
    }

    fun update(dto: TableRegistryUpdateDto): TableRegistryResponseDto {
        val allTables = tableRegistryCache.getAllTables()
        val tableDto = allTables.find { it.id == dto.id } ?: throw RtsGenericException("Table not found")
        val updated = dto.applyTo(tableDto)
        validateRequiredFields(updated)
        validateUniqueness(updated.tableName, updated.defaultPrefix, updated.displayName, updated.id, allTables)
        val saved = tableRegistryCache.save(updated)
        return tableRegistryMapper.toDto(saved)
    }

    private fun validateRequiredFields(dto: TableRegistryDto) {
        StringUtils.requireHasValue(dto.tableName, "Table name is required")
        StringUtils.requireHasValue(dto.defaultPrefix, "Default prefix is required")
        StringUtils.requireHasValue(dto.displayName, "Display name is required")
    }

    private fun validateUniqueness(
        tableName: String,
        defaultPrefix: String,
        displayName: String,
        tableId: UUID,
        allTables: Collection<TableRegistryDto>
    ) {
        if (tableName.isNotBlank()) {
            allTables
                .find { StringUtils.isEquivalent(it.tableName, tableName) && it.id != tableId }
                ?.let { throw RtsGenericException("A Table using the name '$tableName' already exists") }
        }
        if (defaultPrefix.isNotBlank()) {
            allTables
                .find { StringUtils.isEquivalent(it.defaultPrefix, defaultPrefix) && it.id != tableId }
                ?.let { throw RtsGenericException("A Table using the default prefix '$defaultPrefix' already exists") }
        }
        if (displayName.isNotBlank()) {
            allTables
                .find { StringUtils.isEquivalent(it.displayName, displayName) && it.id != tableId }
                ?.let { throw RtsGenericException("A Table using the display name '$displayName' already exists") }
        }
    }
}
