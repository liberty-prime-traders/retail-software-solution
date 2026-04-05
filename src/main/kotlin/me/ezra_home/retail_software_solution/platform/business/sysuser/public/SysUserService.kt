package me.ezra_home.retail_software_solution.platform.business.sysuser.public

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.configuration.session.ServiceAccountContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.platform.business.sysuser.SysUserCache
import me.ezra_home.retail_software_solution.platform.business.sysuser.SysUserDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.SysUserMapper
import me.ezra_home.retail_software_solution.util.enums.ServiceAccount
import org.springframework.stereotype.Service
import java.util.Objects


@Service
class SysUserService(private val sysUserCache: SysUserCache, private val sysUserMapper: SysUserMapper) {

    @TransactionalOnPlatformSchema
    fun addSystemUser(): SysUserWithProfileDto {
        val oktaId = SessionContextProvider.getSession().oktaId
        val systemUser = sysUserCache.getSystemUsers()
            .find { Objects.equals(oktaId, it.oktaId) }
            ?: addSystemUser(oktaId)
        val oktaRecordForNewUser = sysUserCache.getUsersFromOkta().find { Objects.equals(oktaId, it.id) }
        return sysUserMapper.oktaToSystemUser(oktaRecordForNewUser) { systemUser.id }
    }

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getAllUsers(): Collection<SysUserWithProfileDto> = sysUserCache.getAllUsers()

    private fun addSystemUser(oktaId: String?): SysUserDto {
        return ServiceAccountContext.runWithServiceAccount<SysUserDto>(ServiceAccount.RECORD_INITIALIZER) {
            sysUserCache.addSystemUser(SysUserDto(oktaId = oktaId, userType = UserType.END_USER))
        }
    }
}
