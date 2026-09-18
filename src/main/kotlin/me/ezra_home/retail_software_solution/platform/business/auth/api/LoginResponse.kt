package me.ezra_home.retail_software_solution.platform.business.auth.api

import me.ezra_home.retail_software_solution.platform.business.sysuser.api.SysUserWithProfileDto
import me.ezra_home.retail_software_solution.util.enums.RtsRole

data class LoginResponse(
    val sessionToken: String,
    val user: SysUserWithProfileDto,
    val verifiedRoles: List<RtsRole> = emptyList()
)
