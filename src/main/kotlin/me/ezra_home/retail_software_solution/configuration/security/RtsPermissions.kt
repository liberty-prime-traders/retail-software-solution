package me.ezra_home.retail_software_solution.configuration.security

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.organization_admin.api.OrganizationAdminService
import me.ezra_home.retail_software_solution.organizations.business.organization_user.api.OrganizationUserService
import me.ezra_home.retail_software_solution.util.enums.RtsRoleNames
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service("rtsPermissions")
@TransactionalOnPlatformSchema
class RtsPermissions(
    private val organizationAdminService: OrganizationAdminService,
    private val organizationUserService: OrganizationUserService
) {

    fun isPlatformAdmin(): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication.authorities.any { it.authority == "ROLE_${RtsRoleNames.PLATFORM_ADMIN}" }
    }

    fun isOrganizationAdmin(): Boolean {
        return isPlatformAdmin() || organizationAdminService.isOrganizationAdmin()
    }

    fun isOrganizationMember(): Boolean {
        return organizationUserService.isOrganizationMember(SessionContextProvider.getUserId())
    }

}
