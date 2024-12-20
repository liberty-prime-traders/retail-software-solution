package me.ezra_home.retail_software_solution.business.sysuser

import com.okta.sdk.client.Client
import com.okta.sdk.resource.user.UserList
import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.model.entity.SysUserEntity
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
        return sysUsers.map { sysUser ->
            val oktaUser = oktaUsers[sysUser.oktaId]
            sysUserMapper.oktaToSystemUser(oktaUser) {sysUser.id}
        }
    }

    @CacheEvict(allEntries = true)
    fun addSystemUser(userEntity: SysUserEntity): SysUserEntity = userRepository.save(userEntity)
}
