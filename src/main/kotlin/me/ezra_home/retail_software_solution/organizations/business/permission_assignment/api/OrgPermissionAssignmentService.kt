package me.ezra_home.retail_software_solution.organizations.business.permission_assignment.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.cross_tier.authority.GrantedPermission
import me.ezra_home.retail_software_solution.organizations.business.permission_assignment.OrgPermissionAssignmentEntity
import me.ezra_home.retail_software_solution.organizations.business.permission_assignment.OrgPermissionCache
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.enums.RtsPermission
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OrgPermissionAssignmentService(private val orgPermissionCache: OrgPermissionCache) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getPermissions(userId: UUID): Set<RtsPermission> = orgPermissionCache.getOrgPermissions(userId)

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getGrantedPermissions(userId: UUID): List<GrantedPermission> = orgPermissionCache.getGrantedPermissions(userId)

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun holds(userId: UUID, permission: RtsPermission): Boolean = getPermissions(userId).contains(permission)

    @TransactionalOnOrganizationSchema
    fun assignAll(orgUserIdsByUserId: Map<UUID, UUID>, permission: RtsPermission, assignedById: UUID) {
        val entities = orgUserIdsByUserId.filterKeys { userId -> !holds(userId, permission) }
            .map { (userId, orgUserId) ->
                OrgPermissionAssignmentEntity(
                    userId = userId,
                    permission = permission,
                    assignedAt = DateTimes.Offset.Now.system(),
                    assignedById = assignedById,
                    orgUserId = orgUserId
                )
            }
        if (entities.isNotEmpty()) {
            orgPermissionCache.insertAll(entities)
        }
    }

    @TransactionalOnOrganizationSchema
    fun remove(userId: UUID, permission: RtsPermission) {
        orgPermissionCache.remove(userId, permission)
    }

    @TransactionalOnOrganizationSchema
    fun removeAll(userIds: Collection<UUID>, permission: RtsPermission) {
        orgPermissionCache.removeAllHoldingPermission(userIds, permission)
    }

    @TransactionalOnOrganizationSchema
    fun removeAllPermissions(userId: UUID) {
        orgPermissionCache.removeAll(userId)
    }
}
