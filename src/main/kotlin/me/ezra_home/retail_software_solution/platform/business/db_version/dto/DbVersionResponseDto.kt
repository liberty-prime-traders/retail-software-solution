package me.ezra_home.retail_software_solution.platform.business.db_version.dto

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import me.ezra_home.retail_software_solution.configuration.serializer.DatesToMillis
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class DbVersionResponseDto(
    val id: UUID?,
    val versionNumber: String?,
    val sequenceNumber: Long?,
    val prevVersionId: UUID?,
    @JsonSerialize(using = DatesToMillis::class)
    val activatedOn: OffsetDateTime?,
    @JsonSerialize(using = DatesToMillis::class)
    val createdOn: OffsetDateTime?,
    val createdBy: String?
): Serializable
