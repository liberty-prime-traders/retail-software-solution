package me.ezra_home.retail_software_solution.business.userlocation

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.model.entity.UserLocationEntity
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheConfig(cacheNames = [CacheNames.USER_LOCATION])
class UserLocationCache(private val userLocationRepository: UserLocationRepository, ) {

    @Cacheable
    fun getAll(): Collection<UserLocationEntity> {
        return userLocationRepository.findAll()
    }

    @Cacheable
    fun findByLocationId(locationId: UUID): Collection<UserLocationEntity> {
        return userLocationRepository.findByLocationId(locationId)
    }

    @CacheEvict(allEntries = true)
    fun saveAll(userLocationEntities: Collection<UserLocationEntity>) {
         userLocationRepository.saveAll(userLocationEntities)
    }

    @CacheEvict(allEntries = true)
    fun terminateLocationAssignments(locationId: UUID,userIds: Set<UUID>) {
        userLocationRepository.terminateLocationAssignments(locationId, userIds)
    }
}
