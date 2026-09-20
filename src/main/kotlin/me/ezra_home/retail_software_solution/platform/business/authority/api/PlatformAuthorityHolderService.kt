package me.ezra_home.retail_software_solution.platform.business.authority.api

import me.ezra_home.retail_software_solution.cross_tier.authority.AuthorityHolderResponse
import me.ezra_home.retail_software_solution.cross_tier.authority.AuthorityType
import me.ezra_home.retail_software_solution.platform.business.authority.PlatformAuthorityLookup
import me.ezra_home.retail_software_solution.platform.business.permission_assignment.api.PlatformPermissionAssignmentService
import me.ezra_home.retail_software_solution.platform.business.role_assignment.api.PlatformRoleAssignmentService
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import org.springframework.stereotype.Service

@Service
class PlatformAuthorityHolderService(
    private val platformRoleAssignmentService: PlatformRoleAssignmentService,
    private val platformPermissionAssignmentService: PlatformPermissionAssignmentService,
    private val userQualifier: UserQualifier
) {

    fun getHolders(authorityName: String, authorityType: AuthorityType): List<AuthorityHolderResponse> {
        val lookup = PlatformAuthorityLookup.of(authorityName, authorityType)
        val (grants, resolvedName) = when (lookup) {
            is PlatformAuthorityLookup.ByRole ->
                platformRoleAssignmentService.getHolders(lookup.role) to lookup.role.name
            is PlatformAuthorityLookup.ByPermission ->
                platformPermissionAssignmentService.getHolders(lookup.permission) to lookup.permission.name
        }
        return grants.map {
            AuthorityHolderResponse(
                fullName = userQualifier.getUserFullName(it.userId),
                grantedOn = it.grantedAt,
                authorityName = resolvedName
            )
        }
    }
}
