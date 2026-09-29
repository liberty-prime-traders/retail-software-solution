package me.ezra_home.retail_software_solution.locations.business.permission_assignment.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.cross_tier.authority.GrantedPermission
import me.ezra_home.retail_software_solution.locations.business.permission_assignment.LocationPermissionAssignmentEntity
import me.ezra_home.retail_software_solution.locations.business.permission_assignment.LocationPermissionCache
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.enums.RtsPermission
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class LocationPermissionAssignmentService(private val locationPermissionCache: LocationPermissionCache) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun getPermissions(userId: UUID): Set<RtsPermission> = locationPermissionCache.getLocationPermissions(userId)

    @TransactionalOnLocationSchema(readOnly = true)
    fun getGrantedPermissions(userId: UUID): List<GrantedPermission> = locationPermissionCache.getGrantedPermissions(userId)

    @TransactionalOnLocationSchema(readOnly = true)
    fun holds(userId: UUID, permission: RtsPermission): Boolean = getPermissions(userId).contains(permission)

    @TransactionalOnLocationSchema
    fun assignAll(locationUserIdsByUserId: Map<UUID, UUID>, permission: RtsPermission, assignedById: UUID) {
        val entities = locationUserIdsByUserId.filterKeys { userId -> !holds(userId, permission) }
            .map { (userId, locationUserId) ->
                LocationPermissionAssignmentEntity(
                    userId = userId,
                    permission = permission,
                    assignedAt = DateTimes.Offset.Now.system(),
                    assignedById = assignedById,
                    locationUserId = locationUserId
                )
            }
        if (entities.isNotEmpty()) {
            locationPermissionCache.insertAll(entities)
        }
    }

    @TransactionalOnLocationSchema
    fun remove(userId: UUID, permission: RtsPermission) {
        locationPermissionCache.remove(userId, permission)
    }

    @TransactionalOnLocationSchema
    fun removeAll(userIds: Collection<UUID>, permission: RtsPermission) {
        locationPermissionCache.removeAllHoldingPermission(userIds, permission)
    }

    @TransactionalOnLocationSchema
    fun removeAllPermissions(userId: UUID) {
        locationPermissionCache.removeAll(userId)
    }
}
