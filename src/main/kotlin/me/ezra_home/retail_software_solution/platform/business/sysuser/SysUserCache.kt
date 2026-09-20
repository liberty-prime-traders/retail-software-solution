package me.ezra_home.retail_software_solution.platform.business.sysuser

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.FullNames
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.Initials
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.SysUserInsertDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.SysUserWithProfileDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.UserType
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
        return getSystemUsers().map { toProfileDto(it) }
    }

    @Cacheable
    fun getEndUsers(): Collection<SysUserWithProfileDto> {
        return getSystemUsers().filter { it.userType == UserType.END_USER }.map { toProfileDto(it) }
    }

    @CacheEvict(allEntries = true)
    fun create(insertDto: SysUserInsertDto): SysUserDto {
        val saved = userRepository.save(sysUserMapper.toEntity(insertDto))
        return sysUserMapper.toDomainDto(saved)
    }

    private fun toProfileDto(sysUserDto: SysUserDto) = SysUserWithProfileDto(
        id = sysUserDto.id,
        email = sysUserDto.email,
        fullName = FullNames.of(sysUserDto.localFirstName, sysUserDto.localLastName),
        initials = Initials.of(sysUserDto.localFirstName, sysUserDto.localLastName),
        disabledAt = sysUserDto.disabledAt
    )
}
