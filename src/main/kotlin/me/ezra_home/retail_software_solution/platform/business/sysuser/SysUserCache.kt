package me.ezra_home.retail_software_solution.platform.business.sysuser

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.SysUserInsertDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.SysUserWithProfileDto
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
@CacheConfig(cacheNames = [CacheNames.USER])
class SysUserCache(
    private val userRepository: SysUserRepository,
    private val sysUserMapper: SysUserMapper
) {

    @Cacheable
    fun getSystemUsers(): Collection<SysUserDto> {
        return userRepository.findAll().map { sysUserMapper.toDomainDto(it) }
    }

    @Cacheable
    fun getAllUsers(): Collection<SysUserWithProfileDto> {
        return getSystemUsers().map { sysUserMapper.toSysUserWithProfileDto(it) }
    }

    @CacheEvict(allEntries = true)
    fun create(insertDto: SysUserInsertDto): SysUserDto {
        val saved = userRepository.save(sysUserMapper.toEntity(insertDto))
        return sysUserMapper.toDomainDto(saved)
    }
}
