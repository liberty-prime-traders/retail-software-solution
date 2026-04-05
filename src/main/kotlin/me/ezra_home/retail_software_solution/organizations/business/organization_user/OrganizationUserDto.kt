package me.ezra_home.retail_software_solution.organizations.business.organization_user

import java.time.OffsetDateTime
import java.util.UUID

data class OrganizationUserDto(
    var id: UUID? = null,
    var createdById: UUID? = null,
    var createdOn: OffsetDateTime? = null,
    var referenceNumber: String? = null,
    var userId: UUID? = null,
    var endOn: OffsetDateTime? = null,
    var joinRequestId: UUID? = null
) {
    fun isActive(): Boolean = endOn == null
}
