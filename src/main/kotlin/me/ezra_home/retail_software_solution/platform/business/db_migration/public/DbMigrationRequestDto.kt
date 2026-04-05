package me.ezra_home.retail_software_solution.platform.business.db_migration.public

import java.io.Serializable
import java.util.UUID

data class DbMigrationRequestDto(
    val organizationId: UUID,
    val locationIdsToMigrate: Set<UUID>,
    val targetDbVersionId: UUID
) : Serializable
