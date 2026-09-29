package me.ezra_home.retail_software_solution.cross_tier.authority

import me.ezra_home.retail_software_solution.util.enums.RtsPermission
import me.ezra_home.retail_software_solution.util.enums.RtsRole

data class PermissionResponse(
    val permission: RtsPermission,
    val role: RtsRole
)
