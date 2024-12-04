package me.ezra_home.retail_software_solution.business.sysuser

import me.ezra_home.retail_software_solution.model.entity.SysUserEntity
import me.ezra_home.retail_software_solution.rest.session.SessionContextProvider
import org.springframework.stereotype.Service
import java.util.Objects


@Service
class SysUserService(private val sysUserCache: SysUserCache,
                     private val sessionContextProvider: SessionContextProvider,
                     private val sysUserMapper: SysUserMapper) {

    fun addSystemUser(): SysUserDto {
        val oktaId = sessionContextProvider.getSession().oktaId
        val users = sysUserCache.getSystemUsers()
        val systemUser = users.find { Objects.equals(oktaId, it.oktaId) } ?: sysUserCache.addSystemUser(SysUserEntity(oktaId))
        val oktaUser = sysUserCache.getUsersFromOkta().find { Objects.equals(systemUser.oktaId, it.id) }
        return sysUserMapper.oktaToSystemUser(oktaUser)
    }
}
