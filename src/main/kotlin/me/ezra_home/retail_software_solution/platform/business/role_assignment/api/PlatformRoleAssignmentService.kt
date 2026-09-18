package me.ezra_home.retail_software_solution.platform.business.role_assignment.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.role_assignment.PlatformRoleAssignmentEntity
import me.ezra_home.retail_software_solution.platform.business.role_assignment.PlatformRoleCache
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.enums.RtsRole
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PlatformRoleAssignmentService(private val platformRoleCache: PlatformRoleCache) {

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getRoles(userId: UUID): Set<RtsRole> = platformRoleCache.getPlatformRoles(userId)

    @TransactionalOnPlatformSchema(readOnly = true)
    fun holds(userId: UUID, role: RtsRole): Boolean = getRoles(userId).contains(role)

    @TransactionalOnPlatformSchema
    fun assign(userId: UUID, role: RtsRole, assignedById: UUID) {
        if (holds(userId, role)) return
        platformRoleCache.insert(
            PlatformRoleAssignmentEntity(
                userId = userId,
                role = role,
                assignedAt = DateTimes.Offset.Now.system(),
                assignedById = assignedById
            )
        )
    }

    @TransactionalOnPlatformSchema
    fun remove(userId: UUID, role: RtsRole) {
        platformRoleCache.remove(userId, role)
    }
}
