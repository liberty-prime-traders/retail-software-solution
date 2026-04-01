package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.platform.model.DbMigrationEntity
import me.ezra_home.retail_software_solution.util.enums.SchemaOwnerType
import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime
import java.util.UUID

interface DbMigrationRepository : JpaRepository<DbMigrationEntity, UUID> {
    fun findByStartOnBetweenOrderByStartOnDesc(
        start: OffsetDateTime,
        end: OffsetDateTime
    ): Collection<DbMigrationEntity>

    fun findTopBySchemaOwnerIdAndSchemaOwnerTypeAndDbVersionIdOrderByStartOnDesc(
        schemaOwnerId: UUID,
        schemaOwnerType: SchemaOwnerType,
        dbVersionId: UUID
    ): DbMigrationEntity?

    fun findTopBySchemaOwnerIdAndSchemaOwnerTypeAndMigrationParentIdAndStatusOrderByStartOnDesc(
        schemaOwnerId: UUID,
        schemaOwnerType: SchemaOwnerType,
        migrationParentId: UUID,
        status: MigrationStatus
    ): DbMigrationEntity?

    fun findByMigrationParentIdAndSchemaOwnerType(
        migrationParentId: UUID,
        schemaOwnerType: SchemaOwnerType
    ): Collection<DbMigrationEntity>
}
