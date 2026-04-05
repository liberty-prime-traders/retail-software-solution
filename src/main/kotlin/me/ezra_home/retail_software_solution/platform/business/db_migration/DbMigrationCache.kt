package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.platform.business.db_migration.dto.DbMigrationDto
import me.ezra_home.retail_software_solution.platform.business.db_migration.`public`.MigrationStatus
import me.ezra_home.retail_software_solution.platform.business.db_migration.mapping.DbMigrationMapper
import me.ezra_home.retail_software_solution.util.enums.SchemaOwnerType
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.UUID

@Component
@CacheConfig(cacheNames = [CacheNames.DB_MIGRATION])
class DbMigrationCache(
    private val dbMigrationRepository: DbMigrationRepository,
    private val mapper: DbMigrationMapper
) {

    @Cacheable
    fun getAllDbMigrations(): Collection<DbMigrationDto> {
        return dbMigrationRepository.findAll().map { mapper.toDomainDto(it) }
    }

    @Cacheable
    fun getAllDbMigrationsFilteredByDate(
        start: OffsetDateTime,
        end: OffsetDateTime
    ): Collection<DbMigrationDto> {
        return dbMigrationRepository.findByStartOnBetweenOrderByStartOnDesc(
            start = start,
            end = end
        ).map { mapper.toDomainDto(it) }
    }

    @Cacheable
    fun getTopBySchemaOwnerIdAndSchemaOwnerTypeAndDbVersionIdOrderByStartOnDesc(
        schemaOwnerId: UUID,
        schemaOwnerType: SchemaOwnerType,
        dbVersionId: UUID
    ): DbMigrationDto? {
        return dbMigrationRepository.findTopBySchemaOwnerIdAndSchemaOwnerTypeAndDbVersionIdOrderByStartOnDesc(
            schemaOwnerId = schemaOwnerId,
            schemaOwnerType = schemaOwnerType,
            dbVersionId = dbVersionId
        )?.let { mapper.toDomainDto(it) }
    }

    @Cacheable
    fun getLatestFailedLocationMigrationForOrgParent(
        migrationParentId: UUID,
        locationId: UUID
    ): DbMigrationDto? {
        return dbMigrationRepository.findTopBySchemaOwnerIdAndSchemaOwnerTypeAndMigrationParentIdAndStatusOrderByStartOnDesc(
            schemaOwnerId = locationId,
            schemaOwnerType = SchemaOwnerType.LOCATION,
            migrationParentId = migrationParentId,
            status = MigrationStatus.FAILURE
        )?.let { mapper.toDomainDto(it) }
    }

    @Cacheable
    fun getDbLocationMigrationsByMigrationsParentId(
        migrationParentId: UUID,
    ): Collection<DbMigrationDto> {
        return dbMigrationRepository.findByMigrationParentIdAndSchemaOwnerType(
            migrationParentId,
            SchemaOwnerType.LOCATION
        ).map { mapper.toDomainDto(it) }
    }

    @CacheEvict(allEntries = true)
    fun upsertDbMigration(dto: DbMigrationDto) {
        dbMigrationRepository.save(mapper.toEntity(dto))
    }

}
