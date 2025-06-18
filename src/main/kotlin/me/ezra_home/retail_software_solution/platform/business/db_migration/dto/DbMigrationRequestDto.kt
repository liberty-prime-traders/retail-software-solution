package me.ezra_home.retail_software_solution.platform.business.db_migration.dto

import me.ezra_home.retail_software_solution.util.enums.SchemaOwnerType
import java.io.Serializable
import java.util.UUID

data class DbMigrationRequestDto(
    val schemaOwnerId: UUID,
    val schemaOwnerType: SchemaOwnerType
) : Serializable
