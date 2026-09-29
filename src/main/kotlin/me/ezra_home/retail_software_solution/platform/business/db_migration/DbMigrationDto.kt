package me.ezra_home.retail_software_solution.platform.business.db_migration

import me.ezra_home.retail_software_solution.platform.business.db_migration.api.MigrationStatus
import me.ezra_home.retail_software_solution.util.enums.SchemaOwnerType
import java.time.OffsetDateTime
import java.util.UUID

data class DbMigrationDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val dbVersionId: UUID,
    val schemaOwnerId: UUID,
    val schemaOwnerType: SchemaOwnerType,
    val startOn: OffsetDateTime = OffsetDateTime.now(),
    val endOn: OffsetDateTime? = null,
    val status: MigrationStatus,
    val migrationType: MigrationType? = null,
    val message: String? = null,
    val migrationParentId: UUID? = null
)
