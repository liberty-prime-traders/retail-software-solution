package me.ezra_home.retail_software_solution.platform.business.role_assignment.api

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.role_assignment.api.LocationRoleAssignmentService
import me.ezra_home.retail_software_solution.organizations.business.role_assignment.api.OrgRoleAssignmentService
import me.ezra_home.retail_software_solution.util.enums.RtsRole
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Assigns/removes roles with the write-time dependency check and delete-time cascade described in
 * the role authorization design. The org/location tiers can only be consulted when the current
 * request already carries that schema's context (set by the tenant filter) — this service does not
 * fan out across every organization/location schema to look for a user's roles there.
 */
@Service
class RoleResolutionService(
    private val platformRoleAssignmentService: PlatformRoleAssignmentService,
    private val orgRoleAssignmentService: OrgRoleAssignmentService,
    private val locationRoleAssignmentService: LocationRoleAssignmentService
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

    private fun holds(userId: UUID, role: RtsRole): Boolean = when (role.tier) {
        SchemaLevel.PLATFORM -> platformRoleAssignmentService.holds(userId, role)
        SchemaLevel.ORGANIZATION -> orgRoleAssignmentService.holds(userId, role)
        SchemaLevel.LOCATION -> locationRoleAssignmentService.holds(userId, role)
    }

    fun assignRole(userId: UUID, role: RtsRole) {
        val assignedById = SessionContextProvider.getUserId()
        assignRole(userId, role, assignedById)
    }

    private fun assignRole(userId: UUID, role: RtsRole, assignedById: UUID) {
        val missingRequiredRoles = role.requiredRoles.filterNot { holds(userId, it) }
        if (missingRequiredRoles.isNotEmpty()) {
            throw RtsGenericException(
                "Cannot assign role ${role.name} before user gets the following role(s): ${missingRequiredRoles.joinToString { it.name }}"
            )
        }
        when (role.tier) {
            SchemaLevel.PLATFORM -> platformRoleAssignmentService.assign(userId, role, assignedById)
            SchemaLevel.ORGANIZATION -> orgRoleAssignmentService.assign(userId, role, assignedById)
            SchemaLevel.LOCATION -> locationRoleAssignmentService.assign(userId, role, assignedById)
        }
    }

    fun removeRole(userId: UUID, role: RtsRole) {
        removeRoleAndDependents(userId, role)
    }

    private fun removeRoleAndDependents(userId: UUID, role: RtsRole) {
        when (role.tier) {
            SchemaLevel.PLATFORM -> platformRoleAssignmentService.remove(userId, role)
            SchemaLevel.ORGANIZATION -> orgRoleAssignmentService.remove(userId, role)
            SchemaLevel.LOCATION -> locationRoleAssignmentService.remove(userId, role)
        }
        val dependentRoles = getEffectiveRoles(userId).filter { role in it.requiredRoles }
        dependentRoles.forEach { removeRoleAndDependents(userId, it) }
    }
}
