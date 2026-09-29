package me.ezra_home.retail_software_solution.util.business.mappers

import me.ezra_home.retail_software_solution.platform.business.sysuser.api.CreatorFullName
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.NullableFullName
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.SysUserService
import org.springframework.stereotype.Component
import java.util.Objects
import java.util.UUID

@Component
class UserQualifier(private val sysUserService: SysUserService) {

    @NullableFullName
    fun getUserFullName(userId: UUID?): String? {
        if (userId == null) return null
        return findUser(userId)?.fullName
    }

    @CreatorFullName
    fun getCreatorFullName(createdById: UUID): String {
        return findUser(createdById)?.fullName ?: DELETED_USER_LABEL
    }

    private fun findUser(userId: UUID) = sysUserService.getAllUsers().find { Objects.equals(userId, it.id) }

    companion object {
        const val DELETED_USER_LABEL = "Deleted User"
    }
}
