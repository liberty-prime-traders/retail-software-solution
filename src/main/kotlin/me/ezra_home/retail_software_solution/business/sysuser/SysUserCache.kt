package me.ezra_home.retail_software_solution.business.sysuser

import me.ezra_home.retail_software_solution.business.sysuser.okta.OktaUserDto
import me.ezra_home.retail_software_solution.business.util.cache.CacheNames
import me.ezra_home.retail_software_solution.model.entity.SysUserEntity
import me.ezra_home.retail_software_solution.rest.external.okta.OktaFeignClient
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.Objects

@Service
@CacheConfig(cacheNames = [CacheNames.USER])
class SysUserCache(
    private val userRepository: SysUserRepository,
    private val oktaFeignClient: OktaFeignClient,
    private val sysUserMapper: SysUserMapper
) {

    @Cacheable
    fun getSystemUsers(): Collection<SysUserEntity> {
        return userRepository.findAll()
    }

    @Cacheable
    fun getUsersFromOkta(): Collection<OktaUserDto> {
        try {
            return oktaFeignClient.getAllUsers()
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.FAILED_DEPENDENCY, "Unable to fetch users from Okta")
        }
    }

    @Cacheable
    fun getAllUsers(): Collection<SysUserDto> {
        val oktaUsers = getUsersFromOkta()
        val sysUsers = getSystemUsers()
        return oktaUsers.map { oktaUser ->
            val systemId = sysUsers.find { Objects.equals(it.oktaId, oktaUser.id) }?.id
            val sysUserDto = sysUserMapper.oktaToSystemUser(oktaUser)
            sysUserDto.id = systemId
            sysUserDto
        }
    }

    @CacheEvict(allEntries = true)
    fun addSystemUser(userEntity: SysUserEntity): SysUserEntity = userRepository.save(userEntity)
}
