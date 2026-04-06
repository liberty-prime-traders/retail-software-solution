package me.ezra_home.retail_software_solution.platform.business.db_version.api

import me.ezra_home.retail_software_solution.util.model.HasId
import java.time.OffsetDateTime
import java.util.UUID

data class DbVersionDto(
    override var id: UUID? = null,
    var versionNumber: String,
    var sequenceNumber: Long? = null,
    var prevVersionId: UUID? = null,
    var activatedOn: OffsetDateTime? = null
) : HasId
