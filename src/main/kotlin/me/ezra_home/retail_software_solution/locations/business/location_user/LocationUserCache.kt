package me.ezra_home.retail_software_solution.locations.business.location_user

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.locations.business.location_user.api.LocationUserInsertDto
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheSchemaLevel(SchemaLevel.LOCATION)
@CacheConfig(cacheNames = [CacheNames.LOCATION_USER])
class LocationUserCache(
    private val locationUserRepository: LocationUserRepository,
    private val locationUserMapper: LocationUserMapper
) {
    @Cacheable
    fun getLocationUsers(): Collection<LocationUserDto> {
        return locationUserRepository.findAll().map { locationUserMapper.toDomainDto(it) }
    }

    @CacheEvict(allEntries = true)
    fun create(insertDto: LocationUserInsertDto): LocationUserDto {
        val saved = locationUserRepository.save(locationUserMapper.toEntity(insertDto))
        return locationUserMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun save(dto: LocationUserDto): LocationUserDto {
        val saved = locationUserRepository.save(locationUserMapper.toEntity(dto))
        return locationUserMapper.toDomainDto(saved)
    }
}
