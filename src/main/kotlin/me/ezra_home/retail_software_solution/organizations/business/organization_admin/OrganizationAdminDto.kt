package me.ezra_home.retail_software_solution.organizations.business.organization_admin

import java.time.OffsetDateTime
import java.util.UUID

data class OrganizationAdminDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val userId: UUID,
    val endOn: OffsetDateTime? = null,
) {
    fun isActive(): Boolean = endOn == null
}
