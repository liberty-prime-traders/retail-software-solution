package me.ezra_home.retail_software_solution.platform.business.db_version.api

import java.time.OffsetDateTime
import java.util.UUID

data class DbVersionDto(
    val id: UUID,
    val versionNumber: String,
    val sequenceNumber: Long? = null,
    val prevVersionId: UUID? = null,
    val activatedOn: OffsetDateTime? = null
)
