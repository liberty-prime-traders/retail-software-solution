package me.ezra_home.retail_software_solution.platform.business.role_assignment.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.cross_tier.authority.AuthorityGrant
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

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getHolders(role: RtsRole): List<AuthorityGrant> = platformRoleCache.getHolders(role)

    @TransactionalOnPlatformSchema
    fun assignAll(userIds: Collection<UUID>, role: RtsRole, assignedById: UUID) {
        val entities = userIds.filterNot { holds(it, role) }
            .map { userId ->
                PlatformRoleAssignmentEntity(
                    userId = userId,
                    role = role,
                    assignedAt = DateTimes.Offset.Now.system(),
                    assignedById = assignedById
                )
            }
        if (entities.isNotEmpty()) {
            platformRoleCache.insertAll(entities)
        }
    }

    @TransactionalOnPlatformSchema
    fun remove(userId: UUID, role: RtsRole) {
        platformRoleCache.remove(userId, role)
    }

    @TransactionalOnPlatformSchema
    fun removeAll(userIds: Collection<UUID>, role: RtsRole) {
        platformRoleCache.removeAllHoldingRole(userIds, role)
    }
}
