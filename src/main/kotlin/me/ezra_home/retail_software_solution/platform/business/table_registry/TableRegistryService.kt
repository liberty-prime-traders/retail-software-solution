package me.ezra_home.retail_software_solution.platform.business.table_registry

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.table_registry.dto.TableRegistryResponseDto
import me.ezra_home.retail_software_solution.platform.business.table_registry.dto.TableRegistryUpdateDto
import me.ezra_home.retail_software_solution.platform.model.TableRegistryEntity
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
        val entity = allTables.find { it.id == id } ?: throw RtsGenericException("Table not found")
        if (!entity.validated) {
            validateName(entity.tableName)
            entity.validated = true
            tableRegistryCache.upsertTable(entity)
        }
        return tableRegistryMapper.toDto(entity)
    }

    private fun validateName(name: String?) {
        require(TableName.exists(name)) { "Table name '$name' is not recognized in the system" }
    }

     fun update(dto: TableRegistryUpdateDto): TableRegistryResponseDto {
         val id = dto.id ?: throw RtsGenericException("Table Registry id is required for update")
         val allTables = tableRegistryCache.getAllTables()
         val entity = allTables.find { it.id == id } ?: throw RtsGenericException("Table not found")
         tableRegistryMapper.patchEntity(dto, entity)
         val effectiveDefaultPrefix = entity.defaultPrefix
         val effDisplayName = entity.displayName
         validateRequiredFields(entity)
         validateUniqueness(entity.tableName, effectiveDefaultPrefix, effDisplayName, entity.id, allTables)
         tableRegistryCache.upsertTable(entity)
         return tableRegistryMapper.toDto(entity)
     }

    private fun validateRequiredFields(entity: TableRegistryEntity) {
        StringUtils.getValueOrException(entity.tableName, "Table name is required")
        StringUtils.getValueOrException(entity.defaultPrefix, "Default prefix is required")
        StringUtils.getValueOrException(entity.displayName, "Display name is required")
    }

    private fun validateUniqueness(tableName: String?, defaultPrefix: String?, displayName: String?, tableId: UUID?, allTables: Collection<TableRegistryEntity>) {
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
