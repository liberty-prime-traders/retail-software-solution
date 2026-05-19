package me.ezra_home.retail_software_solution.configuration.security

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.organizations.business.organization_admin.api.OrganizationAdminService
import me.ezra_home.retail_software_solution.util.business.PlatformAdmin
import org.springframework.stereotype.Service

@Service("rtsPermissions")
@TransactionalOnPlatformSchema
class RtsPermissions(private val organizationAdminService: OrganizationAdminService) {

    fun isPlatformAdmin(): Boolean {
        return PlatformAdmin.isPlatformAdmin()
    }

    fun isOrganizationAdmin(): Boolean {
        if (isPlatformAdmin()) return true
        return organizationAdminService.isOrganizationAdmin()
    }

}
