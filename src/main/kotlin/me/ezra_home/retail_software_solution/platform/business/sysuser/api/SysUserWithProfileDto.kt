package me.ezra_home.retail_software_solution.platform.business.sysuser.api

import java.time.OffsetDateTime
import java.util.UUID

data class SysUserWithProfileDto(
    val id: UUID,
    val email: String?,
    val fullName: String?,
    val initials: String?,
    val disabledAt: OffsetDateTime?,
)
