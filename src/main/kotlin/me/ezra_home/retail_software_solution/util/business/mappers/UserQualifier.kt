package me.ezra_home.retail_software_solution.util.business.mappers

import me.ezra_home.retail_software_solution.platform.business.sysuser.api.SysUserService
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import org.springframework.stereotype.Component
import java.util.Objects
import java.util.UUID

@Component
class UserQualifier(private val sysUserService: SysUserService) {

    @FullName
    fun getUserFullName(userId: UUID?): String? {
        if (userId == null) return null
        val userDto = sysUserService.getAllUsers().find { Objects.equals(userId, it.id) }
        if (userDto == null) return null
        return listOfNotNull(userDto.firstName, userDto.lastName).joinToString(" ").ifBlank { null }
    }
}
