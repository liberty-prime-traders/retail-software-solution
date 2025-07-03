package me.ezra_home.retail_software_solution.platform.business.db_migration.dto

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import me.ezra_home.retail_software_solution.configuration.serializer.DatesToMillis
import me.ezra_home.retail_software_solution.util.enums.MigrationStatus
import me.ezra_home.retail_software_solution.util.enums.MigrationType
import me.ezra_home.retail_software_solution.util.enums.SchemaOwnerType
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class DbMigrationResponseDto(
    val id: UUID?,
    val dbVersionId: UUID?,
    val schemaOwnerId: UUID?,
    val schemaOwnerType: SchemaOwnerType?,
    val type: MigrationType?,
    val migrationParentId: UUID?,
    @JsonSerialize(using = DatesToMillis::class)
    val startOn: OffsetDateTime?,
    @JsonSerialize(using = DatesToMillis::class)
    val endOn: OffsetDateTime?,
    val status: MigrationStatus,
    val message: String?,
    var locations: List<DbMigrationResponseDto>?
) : Serializable
