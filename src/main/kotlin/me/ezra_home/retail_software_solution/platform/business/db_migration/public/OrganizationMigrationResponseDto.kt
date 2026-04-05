package me.ezra_home.retail_software_solution.platform.business.db_migration.public

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import me.ezra_home.retail_software_solution.configuration.serializer.DatesToMillis
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class OrganizationMigrationResponseDto(
    val id: UUID,
    val organizationName: String,
    val versionNumber: String,
    @field:JsonSerialize(using = DatesToMillis::class)
    val startOn: OffsetDateTime,
    @field:JsonSerialize(using = DatesToMillis::class)
    val endOn: OffsetDateTime?,
    val status: MigrationStatus,
    val message: String?,
    var locations: List<LocationMigrationResponse>?
) : Serializable
