package me.ezra_home.retail_software_solution.business.audit

import java.io.Serializable
import java.time.OffsetDateTime

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.audit.AuditEntity}
 */
data class AuditDto(
    var createdBy: String? = null,
    var lastUpdatedBy: String? = null,
    var createdOn: OffsetDateTime? = null,
    var lastUpdatedOn: OffsetDateTime? = null
) : Serializable
