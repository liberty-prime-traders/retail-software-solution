package me.ezra_home.retail_software_solution.util.business.mappers.userinfo

import me.ezra_home.retail_software_solution.platform.business.sysuser.SysUserCache
import me.ezra_home.retail_software_solution.util.model.AuditableEntity
import me.ezra_home.retail_software_solution.platform.session.SessionContextProvider
import org.mapstruct.AfterMapping
import org.mapstruct.MappingTarget
import org.springframework.stereotype.Component
import java.util.Objects
import java.util.UUID

@Component
class UserQualifier(private val sysUserCache: SysUserCache, ) {

    @FullName
    fun getUserFullName(userId: UUID?): String? {
        if (userId == null) return null
        val userDto = sysUserCache.getAllUsers().find { Objects.equals(userId, it.id) }
        if (userDto == null) return null
        return "${userDto.firstName} ${userDto.lastName}"
    }

    @CreatedBy
    @AfterMapping
    fun setCreatedBy(@MappingTarget auditableEntity: AuditableEntity) {
        auditableEntity.createdById = SessionContextProvider.getUserId()
    }
}
