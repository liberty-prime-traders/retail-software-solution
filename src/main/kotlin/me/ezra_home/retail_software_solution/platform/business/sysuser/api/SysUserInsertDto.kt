package me.ezra_home.retail_software_solution.platform.business.sysuser.api

import java.io.Serializable

data class SysUserInsertDto(
    val email: String? = null,
    val localFirstName: String,
    val localLastName: String? = null,
    val userType: UserType? = null
) : Serializable
