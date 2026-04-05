package me.ezra_home.retail_software_solution.platform.business.db_migration.dto

import me.ezra_home.retail_software_solution.platform.business.db_migration.`public`.MigrationStatus
import me.ezra_home.retail_software_solution.platform.business.db_migration.MigrationType
import me.ezra_home.retail_software_solution.util.enums.SchemaOwnerType
import me.ezra_home.retail_software_solution.util.model.HasId
import java.time.OffsetDateTime
import java.util.UUID

data class DbMigrationDto(
    override var id: UUID? = null,
    var createdById: UUID? = null,
    var createdOn: OffsetDateTime? = null,
    var referenceNumber: String? = null,
    var dbVersionId: UUID,
    var schemaOwnerId: UUID,
    var schemaOwnerType: SchemaOwnerType,
    var startOn: OffsetDateTime = OffsetDateTime.now(),
    var endOn: OffsetDateTime? = null,
    var status: MigrationStatus,
    var migrationType: MigrationType? = null,
    var message: String? = null,
    var migrationParentId: UUID? = null
) : HasId
