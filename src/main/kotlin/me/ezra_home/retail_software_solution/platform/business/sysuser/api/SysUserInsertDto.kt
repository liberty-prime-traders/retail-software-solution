package me.ezra_home.retail_software_solution.platform.business.sysuser.api

import java.io.Serializable

data class SysUserInsertDto(
    val oktaId: String? = null,
    val userType: UserType? = null
) : Serializable
