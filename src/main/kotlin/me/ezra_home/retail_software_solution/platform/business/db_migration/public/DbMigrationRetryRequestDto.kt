package me.ezra_home.retail_software_solution.platform.business.db_migration.public

import java.io.Serializable
import java.util.UUID

data class DbMigrationRetryRequestDto(
    val orgMigrationId: UUID,
    val locationIdsToMigrate: Set<UUID>
) : Serializable
