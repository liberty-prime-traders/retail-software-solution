package me.ezra_home.retail_software_solution.platform.business.sysuser.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.configuration.session.ServiceAccountContext
import me.ezra_home.retail_software_solution.platform.business.sysuser.SysUserCache
import me.ezra_home.retail_software_solution.platform.business.sysuser.SysUserDto
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.enums.ServiceAccount
import me.ezra_home.retail_software_solution.util.exceptions.AuthException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SysUserService(
    private val sysUserCache: SysUserCache
) {

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getAllUsers(): Collection<SysUserWithProfileDto> = sysUserCache.getAllUsers()

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getEndUsers(): Collection<SysUserWithProfileDto> = sysUserCache.getEndUsers()

    @TransactionalOnPlatformSchema(readOnly = true)
    fun findByEmail(email: String): SysUserWithProfileDto? = getAllUsers().find { it.email == email }

    @TransactionalOnPlatformSchema(readOnly = true)
    fun findById(userId: UUID): SysUserWithProfileDto? = getAllUsers().find { it.id == userId }

    @TransactionalOnPlatformSchema(readOnly = true)
    fun throwIfAccountIsDisabled(userId: UUID) {
        sysUserCache.getSystemUsers()
            .find { it.id == userId }?.disabledAt
            ?.run { throw AuthException.AccountDisabled() }
    }

    @TransactionalOnPlatformSchema
    fun createUser(email: String?, localFirstName: String, localLastName: String?): UUID {
        StringUtils.requireHasValue(localFirstName, "A SysUser must have a local first name")
        val created = ServiceAccountContext.runWithServiceAccount<SysUserDto>(ServiceAccount.RECORD_INITIALIZER) {
            sysUserCache.create(
                SysUserInsertDto(
                    email = email,
                    localFirstName = localFirstName,
                    localLastName = localLastName,
                    userType = UserType.END_USER
                )
            )
        }
        return created.id
    }
}
