package me.ezra_home.retail_software_solution.cross_tier.authority

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.permission_assignment.api.LocationPermissionAssignmentService
import me.ezra_home.retail_software_solution.locations.business.role_assignment.api.LocationRoleAssignmentService
import me.ezra_home.retail_software_solution.organizations.business.permission_assignment.api.OrgPermissionAssignmentService
import me.ezra_home.retail_software_solution.organizations.business.role_assignment.api.OrgRoleAssignmentService
import me.ezra_home.retail_software_solution.platform.business.permission_assignment.api.PlatformPermissionAssignmentService
import me.ezra_home.retail_software_solution.platform.business.role_assignment.api.PlatformRoleAssignmentService
import me.ezra_home.retail_software_solution.util.enums.RtsPermission
import me.ezra_home.retail_software_solution.util.enums.RtsRole
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class EffectiveAuthorizationService(
    private val platformRoleAssignmentService: PlatformRoleAssignmentService,
    private val orgRoleAssignmentService: OrgRoleAssignmentService,
    private val locationRoleAssignmentService: LocationRoleAssignmentService,
    private val platformPermissionAssignmentService: PlatformPermissionAssignmentService,
    private val orgPermissionAssignmentService: OrgPermissionAssignmentService,
    private val locationPermissionAssignmentService: LocationPermissionAssignmentService
) {

    fun getEffectiveRoles(userId: UUID): Set<RtsRole> {
        val roles = mutableSetOf<RtsRole>()
        roles += platformRoleAssignmentService.getRoles(userId)
        if (SessionContextProvider.getOrganizationIdOrNull() != null) {
            roles += orgRoleAssignmentService.getRoles(userId)
        }
        if (SessionContextProvider.getLocationIdOrNull() != null) {
            roles += locationRoleAssignmentService.getRoles(userId)
        }
        return roles
    }

    fun getEffectivePermissions(userId: UUID): Set<RtsPermission> {
        val fromRoles = getEffectiveRoles(userId).flatMap { it.permissions }
        val directPlatform = platformPermissionAssignmentService.getPermissions(userId)
        val directOrg = if (SessionContextProvider.getOrganizationIdOrNull() != null) {
            orgPermissionAssignmentService.getPermissions(userId)
        } else {
            emptySet()
        }
        val directLocation = if (SessionContextProvider.getLocationIdOrNull() != null) {
            locationPermissionAssignmentService.getPermissions(userId)
        } else {
            emptySet()
        }
        return (fromRoles + directPlatform + directOrg + directLocation).toSet()
    }
}
