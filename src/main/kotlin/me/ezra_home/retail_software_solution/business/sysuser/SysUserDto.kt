package me.ezra_home.retail_software_solution.business.sysuser

import me.ezra_home.retail_software_solution.model.enums.Status
import java.io.Serializable
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.SysUserEntity}
 */
data class SysUserDto(
    var id: UUID? = null,
    var status: Status?,
    var oktaId: String? = null,
    var firstName: String,
    var lastName: String,
    var mobilePhone: String?,
    var secondEmail: String?,
    var login: String,
    var email: String
) : Serializable
