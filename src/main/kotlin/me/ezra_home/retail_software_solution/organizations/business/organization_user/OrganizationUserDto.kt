package me.ezra_home.retail_software_solution.organizations.business.organization_user

import java.time.OffsetDateTime
import java.util.UUID

data class OrganizationUserDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val userId: UUID,
    val endOn: OffsetDateTime? = null,
    val joinRequestId: UUID? = null
) {
    fun isActive(): Boolean = endOn == null
}
