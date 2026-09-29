package me.ezra_home.retail_software_solution.locations.business.permission_assignment

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.cross_tier.authority.GrantedPermission
import me.ezra_home.retail_software_solution.util.enums.RtsPermission
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheSchemaLevel(SchemaLevel.LOCATION)
@CacheConfig(cacheNames = [CacheNames.LOCATION_PERMISSIONS])
class LocationPermissionCache(private val locationPermissionAssignmentRepository: LocationPermissionAssignmentRepository) {

    @Cacheable
    fun getLocationPermissions(userId: UUID): Set<RtsPermission> {
        return locationPermissionAssignmentRepository.findAllByUserId(userId).map { it.permission }.toSet()
    }

    @Cacheable
    fun getGrantedPermissions(userId: UUID): List<GrantedPermission> {
        return locationPermissionAssignmentRepository.findAllByUserId(userId).map { GrantedPermission(it.permission, it.assignedAt) }
    }

    @CacheEvict(allEntries = true)
    fun insertAll(locationPermissionAssignmentEntities: Collection<LocationPermissionAssignmentEntity>) {
        locationPermissionAssignmentRepository.saveAll(locationPermissionAssignmentEntities)
    }

    @CacheEvict(allEntries = true)
    fun remove(userId: UUID, permission: RtsPermission) {
        locationPermissionAssignmentRepository.findByUserIdAndPermission(userId, permission)
            ?.let { locationPermissionAssignmentRepository.delete(it) }
    }

    @CacheEvict(allEntries = true)
    fun removeAllHoldingPermission(userIds: Collection<UUID>, permission: RtsPermission) {
        locationPermissionAssignmentRepository.deleteAll(
            locationPermissionAssignmentRepository.findAllByUserIdInAndPermission(userIds, permission)
        )
    }

    @CacheEvict(allEntries = true)
    fun removeAll(userId: UUID) {
        locationPermissionAssignmentRepository.deleteAll(locationPermissionAssignmentRepository.findAllByUserId(userId))
    }
}
