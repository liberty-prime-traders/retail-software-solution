package me.ezra_home.retail_software_solution.locations.business.role_assignment.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.role_assignment.LocationRoleAssignmentEntity
import me.ezra_home.retail_software_solution.locations.business.role_assignment.LocationRoleCache
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.enums.RtsRole
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class LocationRoleAssignmentService(private val locationRoleCache: LocationRoleCache) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun getRoles(userId: UUID): Set<RtsRole> = locationRoleCache.getLocationRoles(userId)

    @TransactionalOnLocationSchema(readOnly = true)
    fun holds(userId: UUID, role: RtsRole): Boolean = getRoles(userId).contains(role)

    @TransactionalOnLocationSchema
    fun assign(userId: UUID, locationUserId: UUID, role: RtsRole, assignedById: UUID) {
        if (holds(userId, role)) return
        locationRoleCache.insert(
            LocationRoleAssignmentEntity(
                userId = userId,
                role = role,
                assignedAt = DateTimes.Offset.Now.system(),
                assignedById = assignedById,
                locationUserId = locationUserId
            )
        )
    }

    @TransactionalOnLocationSchema
    fun remove(userId: UUID, role: RtsRole) {
        locationRoleCache.remove(userId, role)
    }

    @TransactionalOnLocationSchema
    fun removeAllRoles(userId: UUID) {
        locationRoleCache.removeAll(userId)
    }
}
