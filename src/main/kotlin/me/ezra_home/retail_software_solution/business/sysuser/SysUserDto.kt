package me.ezra_home.retail_software_solution.business.sysuser

import com.okta.sdk.resource.user.UserStatus
import java.io.Serializable
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.SysUserEntity}
 */
data class SysUserDto(
    val id: UUID,
    val status: UserStatus,
    val oktaId: String,
    val firstName: String,
    val lastName: String,
    val mobilePhone: String?,
    val secondEmail: String?,
    val email: String
) : Serializable
