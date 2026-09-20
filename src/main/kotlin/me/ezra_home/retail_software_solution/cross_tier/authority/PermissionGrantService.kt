package me.ezra_home.retail_software_solution.cross_tier.authority

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.location_user.api.LocationUserService
import me.ezra_home.retail_software_solution.locations.business.permission_assignment.api.LocationPermissionAssignmentService
import me.ezra_home.retail_software_solution.organizations.business.organization_user.api.OrganizationUserService
import me.ezra_home.retail_software_solution.organizations.business.permission_assignment.api.OrgPermissionAssignmentService
import me.ezra_home.retail_software_solution.platform.business.permission_assignment.api.PlatformPermissionAssignmentService
import me.ezra_home.retail_software_solution.util.enums.RtsPermission
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.stereotype.Service
import java.util.UUID

// Add-on grants are independent of roles and survive a role revocation.
@Service
class PermissionGrantService(
    private val organizationUserService: OrganizationUserService,
    private val locationUserService: LocationUserService,
    private val platformPermissionAssignmentService: PlatformPermissionAssignmentService,
    private val orgPermissionAssignmentService: OrgPermissionAssignmentService,
    private val locationPermissionAssignmentService: LocationPermissionAssignmentService
) {

    fun assignPermission(userId: UUID, permission: RtsPermission) {
        assignPermissions(listOf(userId), listOf(permission))
    }

    fun assignPermissions(userIds: Collection<UUID>, permissions: Collection<RtsPermission>) {
        if (userIds.isEmpty() || permissions.isEmpty()) return
        val assignedById = SessionContextProvider.getUserId()
        permissions.forEach { permission ->
            when (permission.tier) {
                SchemaLevel.PLATFORM -> platformPermissionAssignmentService.assignAll(userIds, permission, assignedById)
                SchemaLevel.ORGANIZATION -> {
                    val orgUserIdsByUserId = organizationUserService.getActiveMembershipIds(userIds)
                    orgPermissionAssignmentService.assignAll(orgUserIdsByUserId, permission, assignedById)
                }
                SchemaLevel.LOCATION -> {
                    val locationUserIdsByUserId = locationUserService.getActiveMembershipIds(userIds)
                    locationPermissionAssignmentService.assignAll(locationUserIdsByUserId, permission, assignedById)
                }
            }
        }
    }

    fun removePermission(userId: UUID, permission: RtsPermission) {
        removePermissions(listOf(userId), listOf(permission))
    }

    fun removePermissions(userIds: Collection<UUID>, permissions: Collection<RtsPermission>) {
        if (userIds.isEmpty() || permissions.isEmpty()) return
        permissions.forEach { permission ->
            when (permission.tier) {
                SchemaLevel.PLATFORM -> platformPermissionAssignmentService.removeAll(userIds, permission)
                SchemaLevel.ORGANIZATION -> orgPermissionAssignmentService.removeAll(userIds, permission)
                SchemaLevel.LOCATION -> locationPermissionAssignmentService.removeAll(userIds, permission)
            }
        }
    }
}
