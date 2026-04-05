package me.ezra_home.retail_software_solution.platform.business.table_registry.public

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.table_registry.TableRegistryCache
import me.ezra_home.retail_software_solution.platform.business.table_registry.TableRegistryMapper
import me.ezra_home.retail_software_solution.platform.business.table_registry.dto.TableRegistryDto
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
    fun getAll(): Collection<TableRegistryResponseDto> =
        tableRegistryCache.getAllTables().map { tableRegistryMapper.toDto(it) }

    fun validate(id: UUID): TableRegistryResponseDto {
        val allTables = tableRegistryCache.getAllTables()
        val dto = allTables.find { it.id == id } ?: throw RtsGenericException("Table not found")
        if (!dto.validated) {
            validateName(dto.tableName)
            dto.validated = true
            tableRegistryCache.upsertTable(dto)
        }
        return tableRegistryMapper.toDto(dto)
    }

    private fun validateName(name: String?) {
        require(TableName.exists(name)) { "Table name '$name' is not recognized in the system" }
    }

    fun update(dto: TableRegistryUpdateDto): TableRegistryResponseDto {
        val id = dto.id
        val allTables = tableRegistryCache.getAllTables()
        val tableDto = allTables.find { it.id == id } ?: throw RtsGenericException("Table not found")
        tableRegistryMapper.patchDto(dto, tableDto)
        val effectiveDefaultPrefix = tableDto.defaultPrefix
        val effDisplayName = tableDto.displayName
        validateRequiredFields(tableDto)
        validateUniqueness(tableDto.tableName, effectiveDefaultPrefix, effDisplayName, tableDto.id, allTables)
        tableRegistryCache.upsertTable(tableDto)
        return tableRegistryMapper.toDto(tableDto)
    }

    private fun validateRequiredFields(dto: TableRegistryDto) {
        StringUtils.getValueOrException(dto.tableName, "Table name is required")
        StringUtils.getValueOrException(dto.defaultPrefix, "Default prefix is required")
        StringUtils.getValueOrException(dto.displayName, "Display name is required")
    }

    private fun validateUniqueness(
        tableName: String?,
        defaultPrefix: String?,
        displayName: String?,
        tableId: UUID?,
        allTables: Collection<TableRegistryDto>
    ) {
        if (!tableName.isNullOrBlank()) {
            allTables
                .find { StringUtils.isEquivalent(it.tableName, tableName) && it.id != tableId }
                ?.let { throw RtsGenericException("A Table using the name '$tableName' already exists") }
        }
        if (!defaultPrefix.isNullOrBlank()) {
            allTables
                .find { StringUtils.isEquivalent(it.defaultPrefix, defaultPrefix) && it.id != tableId }
                ?.let { throw RtsGenericException("A Table using the default prefix '$defaultPrefix' already exists") }
        }
        if (!displayName.isNullOrBlank()) {
            allTables
                .find { StringUtils.isEquivalent(it.displayName, displayName) && it.id != tableId }
                ?.let { throw RtsGenericException("A Table using the display name '$displayName' already exists") }
        }
    }

}
