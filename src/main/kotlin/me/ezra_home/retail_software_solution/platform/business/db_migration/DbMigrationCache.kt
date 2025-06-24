package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.platform.model.DbMigrationEntity
import me.ezra_home.retail_software_solution.util.enums.SchemaOwnerType
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.UUID

@Component
@CacheConfig(cacheNames = [CacheNames.DB_MIGRATION])
class DbMigrationCache(private val dbMigrationRepository: DbMigrationRepository) {

    @Cacheable
    fun getAllDbMigrationsFilteredByDate(
        start: OffsetDateTime,
        end: OffsetDateTime
    ): Collection<DbMigrationEntity> {
        return dbMigrationRepository.findByStartOnBetweenOrderByStartOnDesc(
            start = start,
            end = end
        )
    }

    @Cacheable
    fun getTopBySchemaOwnerIdAndSchemaOwnerTypeAndDbVersionIdOrderByStartOnDesc(
        schemaOwnerId: UUID,
        schemaOwnerType: SchemaOwnerType,
        dbVersionId: UUID
    ): DbMigrationEntity? {
        return dbMigrationRepository.findTopBySchemaOwnerIdAndSchemaOwnerTypeAndDbVersionIdOrderByStartOnDesc(
            schemaOwnerId = schemaOwnerId,
            schemaOwnerType = schemaOwnerType,
            dbVersionId = dbVersionId
        )
    }

    @CacheEvict(allEntries = true)
    fun upsertDbMigration(dbMigrationEntity: DbMigrationEntity) {
        dbMigrationRepository.save(dbMigrationEntity)
    }

}
