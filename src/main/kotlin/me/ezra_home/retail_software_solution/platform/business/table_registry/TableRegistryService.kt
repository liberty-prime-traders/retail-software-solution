package me.ezra_home.retail_software_solution.platform.business.table_registry

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.table_registry.dto.TableRegistryInsertDto
import me.ezra_home.retail_software_solution.platform.business.table_registry.dto.TableRegistryResponseDto
import me.ezra_home.retail_software_solution.platform.business.table_registry.dto.TableRegistryUpdateDto
import me.ezra_home.retail_software_solution.platform.model.TableRegistryEntity
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
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
        tableRegistryCache.getAllTableRegistries().map { tableRegistryMapper.toDto(it) }

    fun create(dto: TableRegistryInsertDto): TableRegistryResponseDto {
        val tableName = StringUtils.getValueOrException(dto.tableName, "Table name is required")
        val defaultPrefix = StringUtils.getValueOrException(dto.defaultPrefix, "Default prefix is required")
        val minimumVersionId = dto.minimumVersionId ?: throw RtsGenericException("Minimum version id is required")
        val schemaLevel = dto.schemaLevel ?: throw RtsGenericException("Schema level is required")
        val displayName = StringUtils.getValueOrException(dto.displayName, "Display name is required")
        val description = StringUtils.getValueOrException(dto.description, "Description is required")
        val userFacing = dto.userFacing ?: false

        validateUniqueness(tableName, defaultPrefix, displayName, null)

        val validatedDto = TableRegistryInsertDto(tableName, defaultPrefix, minimumVersionId, schemaLevel, displayName, description, userFacing)
        val entity: TableRegistryEntity = tableRegistryMapper.toEntity(validatedDto).apply { createdById = SessionContextProvider.getUserId() }

        tableRegistryCache.upsertTableRegistry(entity)
        return tableRegistryMapper.toDto(entity)
    }

     fun update(dto: TableRegistryUpdateDto): TableRegistryResponseDto {
         val id = dto.id ?: throw RtsGenericException("Registry id is required")
         val entity = tableRegistryCache.getAllTableRegistries().find { it.id == id } ?: throw RtsGenericException("Registry not found")
         val effectiveTableName = if (StringUtils.hasValue(dto.tableName)) dto.tableName?.get() else null
         val effectiveDefaultPrefix = if (StringUtils.hasValue(dto.defaultPrefix)) dto.defaultPrefix?.get() else null
         val effDisplayName = if (StringUtils.hasValue(dto.displayName)) dto.displayName?.get() else null
         validateUniqueness(effectiveTableName, effectiveDefaultPrefix, effDisplayName, entity.id)

         tableRegistryMapper.updateEntity(dto, entity)
         tableRegistryCache.upsertTableRegistry(entity)
         return tableRegistryMapper.toDto(entity)
     }

    private fun validateUniqueness(tableName: String?, defaultPrefix: String?, displayName: String?, ignoreId: UUID?) {
        if (!tableName.isNullOrBlank()) {
            tableRegistryCache.getAllTableRegistries()
                .find { StringUtils.isEquivalent(it.tableName, tableName) && it.id != ignoreId }
                ?.let { throw RtsGenericException("A Table Registry using the table name '$tableName' already exists") }
        }
        if (!defaultPrefix.isNullOrBlank()) {
            tableRegistryCache.getAllTableRegistries()
                .find { StringUtils.isEquivalent(it.defaultPrefix, defaultPrefix) && it.id != ignoreId }
                ?.let { throw RtsGenericException("A Table Registry using the default prefix '$defaultPrefix' already exists") }
        }
        if (!displayName.isNullOrBlank()) {
            tableRegistryCache.getAllTableRegistries()
                .find { StringUtils.isEquivalent(it.displayName, displayName) && it.id != ignoreId }
                ?.let { throw RtsGenericException("A Table Registry using the display name '$displayName' already exists") }
        }
    }

    fun delete(id: UUID?) {
        id?.let { tableRegistryCache.deleteTableRegistry(it) }
    }

    fun getAllForSchemaLevel(schemaLevel: SchemaLevel): List<TableRegistryEntity> {
        return tableRegistryCache.getAllTableRegistries()
            .filter { it.schemaLevel == schemaLevel }
    }

    fun getTableRegistryForTableName(tableName: String): TableRegistryEntity {
        return tableRegistryCache.findByTableName(tableName)
            ?: throw RtsGenericException("Table registry not found for table: $tableName")
    }
}
