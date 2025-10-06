package me.ezra_home.retail_software_solution.locations.business.prefix_configuration

import me.ezra_home.retail_software_solution.business.prefix_configuration.shared.dto.PrefixConfigurationResponseDto
import me.ezra_home.retail_software_solution.business.prefix_configuration.shared.dto.PrefixConfigurationUpdateDto
import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.model.LocationPrefixConfigurationEntity
import me.ezra_home.retail_software_solution.platform.business.table_registry.TableRegistryService
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class LocationPrefixConfigurationService(
    private val locationPrefixConfigurationCache: LocationPrefixConfigurationCache,
    private val tableRegistryService: TableRegistryService,
    private val locationPrefixConfigurationMapper: LocationPrefixConfigurationMapper
) {
    @TransactionalOnLocationSchema(readOnly = true)
    fun getForTableRegistry(tableRegistryId: UUID): Collection<PrefixConfigurationResponseDto> {
        return locationPrefixConfigurationCache.getForTableRegistry(tableRegistryId)
            .map { locationPrefixConfigurationMapper.toDto(it) }
    }

    fun updatePrefix(dto: PrefixConfigurationUpdateDto): PrefixConfigurationResponseDto {
        val entity = locationPrefixConfigurationCache.getById(dto.prefixConfigurationId)
            ?: throw RtsGenericException("Prefix configuration not found")
        entity.prefix = dto.prefix
        return locationPrefixConfigurationMapper.toDto(locationPrefixConfigurationCache.upsertPrefixConfiguration(entity))
    }

    fun createForRegistry(tableRegistryId: UUID, defaultPrefix: String, userId: UUID) {
        val entity = LocationPrefixConfigurationEntity(
            tableRegistryId = tableRegistryId,
            prefix = defaultPrefix
        ).apply {
            createdById = userId
            createdOn = OffsetDateTime.now()
        }
        locationPrefixConfigurationCache.upsertPrefixConfiguration(entity)
    }

    fun getPrefixForTableName(tableName: String): String? {
        val tableRegistry = tableRegistryService.getTableRegistryForTableName(tableName)
        val prefixConfig = getPrefixConfigurationForTableRegistry(tableRegistry.id!!)
        return prefixConfig?.prefix ?: tableRegistry.defaultPrefix
    }

    @TransactionalOnLocationSchema(readOnly = true)
    fun getPrefixConfigurationForTableRegistry(tableRegistryId: UUID): LocationPrefixConfigurationEntity? {
        return locationPrefixConfigurationCache.getForTableRegistry(tableRegistryId)
            .firstOrNull()
    }

    @TransactionalOnLocationSchema(readOnly = true)
    fun getPrefixConfigurationForTable(tableName: String): LocationPrefixConfigurationEntity? {
        val tableRegistry = tableRegistryService.getTableRegistryForTableName(tableName)
        return getPrefixConfigurationForTableRegistry(tableRegistry.id!!)
    }
}
