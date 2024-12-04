package me.ezra_home.retail_software_solution.business.util.mapper.userinfo

import me.ezra_home.retail_software_solution.business.sysuser.SysUserCache
import org.springframework.stereotype.Component
import java.util.Objects
import java.util.UUID

@Component
class UserQualifier(private val sysUserCache: SysUserCache) {

    @FullName
    fun getUserFullName(userId: UUID?): String? {
        if (userId == null) return null
        val userDto = sysUserCache.getAllUsers().find { Objects.equals(userId, it.id) }
        if (userDto == null) return null
        return "${userDto.firstName} ${userDto.lastName}"
    }
}
