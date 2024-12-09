package me.ezra_home.retail_software_solution.business.sysuser

import me.ezra_home.retail_software_solution.model.entity.SysUserEntity
import me.ezra_home.retail_software_solution.rest.session.SessionContextProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Objects


@Service
class SysUserService(private val sysUserCache: SysUserCache,
                     private val sessionContextProvider: SessionContextProvider,
                     private val sysUserMapper: SysUserMapper) {

    @Transactional
    fun addSystemUser(): SysUserDto {
        val oktaId = sessionContextProvider.getSession().oktaId
        val allSystemUserRecords = sysUserCache.getSystemUsers()
        val systemUser = allSystemUserRecords.find { Objects.equals(oktaId, it.oktaId) }
            ?: sysUserCache.addSystemUser(SysUserEntity(oktaId))
        val oktaRecordForNewUser = sysUserCache.getUsersFromOkta().find { Objects.equals(oktaId, it.id) }
        return sysUserMapper.oktaToSystemUser(oktaRecordForNewUser) { systemUser.id }
    }

    @Transactional
    fun getAllUsers(): Collection<SysUserDto> = sysUserCache.getAllUsers()
}
