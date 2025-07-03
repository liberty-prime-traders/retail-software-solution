package me.ezra_home.retail_software_solution.platform.business.db_migration.dto

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import me.ezra_home.retail_software_solution.configuration.serializer.DatesToMillis
import me.ezra_home.retail_software_solution.util.enums.MigrationStatus
import me.ezra_home.retail_software_solution.util.enums.MigrationType
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class MigrationHistoryResponseDto(
    val organizationId: UUID?,
    val organizationName: String?,
    val versionNumber: String?,
    val migrationParentId: UUID?,
    @JsonSerialize(using = DatesToMillis::class)
    val startDate: OffsetDateTime?,
    @JsonSerialize(using = DatesToMillis::class)
    val endDate: OffsetDateTime?,
    val type: MigrationType?,
    val status: MigrationStatus?,
    val message: String?,
    val locations: List<LocationMigrationResponse>
) : Serializable
