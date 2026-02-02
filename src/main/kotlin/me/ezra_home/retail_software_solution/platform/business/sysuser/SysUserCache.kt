package me.ezra_home.retail_software_solution.platform.business.sysuser

import com.okta.sdk.client.Client
import com.okta.sdk.resource.user.UserList
import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.SysUserMapper
import me.ezra_home.retail_software_solution.platform.model.SysUserEntity
import me.ezra_home.retail_software_solution.util.enums.UserType
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.Collections


@Service
@CacheConfig(cacheNames = [CacheNames.USER])
class SysUserCache(
    private val userRepository: SysUserRepository,
    private val sysUserMapper: SysUserMapper,
    private val oktaClient: Client
) {

    @Cacheable
    fun getSystemUsers(): Collection<SysUserEntity> {
        return userRepository.findAll()
    }

    @Cacheable
    fun getUsersFromOkta(): UserList {
        try {
            return oktaClient.listUsers()
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to fetch users from Okta", e)
        }
    }

    @Cacheable
    fun getAllUsers(): Collection<SysUserDto> {
        val sysUsers = getSystemUsers()
        if (sysUsers.isEmpty()) return Collections.emptyList()
        val oktaUsers = getUsersFromOkta().associateBy { it.id }
        val systemEndUsers = sysUsers.filter { it.userType == UserType.END_USER }.map { sysUser ->
            val oktaUser = oktaUsers[sysUser.oktaId]
            sysUserMapper.oktaToSystemUser(oktaUser) {sysUser.id}
        }
        val serviceAccounts = sysUsers.filter { it.userType == UserType.SERVICE_ACCOUNT }.map { sysUser ->
            sysUserMapper.sysUserEntityToSysUserDto(sysUser)
        }
        return systemEndUsers + serviceAccounts
    }

    @CacheEvict(allEntries = true)
    fun addSystemUser(userEntity: SysUserEntity): SysUserEntity = userRepository.save(userEntity)
}
