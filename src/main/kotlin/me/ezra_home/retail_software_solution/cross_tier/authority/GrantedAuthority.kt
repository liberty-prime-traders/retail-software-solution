package me.ezra_home.retail_software_solution.cross_tier.authority

import me.ezra_home.retail_software_solution.util.enums.RtsPermission
import me.ezra_home.retail_software_solution.util.enums.RtsRole
import java.time.OffsetDateTime

data class GrantedRole(
    val role: RtsRole,
    val grantedAt: OffsetDateTime
)

data class GrantedPermission(
    val permission: RtsPermission,
    val grantedAt: OffsetDateTime
)
