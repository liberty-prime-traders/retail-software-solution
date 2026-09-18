package me.ezra_home.retail_software_solution.locations.business.role_assignment

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.util.enums.RtsRole
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheSchemaLevel(SchemaLevel.LOCATION)
@CacheConfig(cacheNames = [CacheNames.LOCATION_ROLES])
class LocationRoleCache(private val locationRoleAssignmentRepository: LocationRoleAssignmentRepository) {

    @Cacheable
    fun getLocationRoles(userId: UUID): Set<RtsRole> {
        return locationRoleAssignmentRepository.findAllByUserId(userId).map { it.role }.toSet()
    }

    @CacheEvict(allEntries = true)
    fun insert(locationRoleAssignmentEntity: LocationRoleAssignmentEntity) {
        locationRoleAssignmentRepository.save(locationRoleAssignmentEntity)
    }

    @CacheEvict(allEntries = true)
    fun remove(userId: UUID, role: RtsRole) {
        locationRoleAssignmentRepository.findByUserIdAndRole(userId, role)
            ?.let { locationRoleAssignmentRepository.delete(it) }
    }
}
