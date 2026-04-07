package me.ezra_home.retail_software_solution.platform.business.sysuser

import me.ezra_home.retail_software_solution.platform.business.sysuser.api.UserType
import java.time.OffsetDateTime
import java.util.UUID

data class SysUserDto(
    val id: UUID,
    val createdById: UUID? = null,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val oktaId: String? = null,
    val localFirstName: String? = null,
    val localLastName: String? = null,
    val userType: UserType? = null
)
