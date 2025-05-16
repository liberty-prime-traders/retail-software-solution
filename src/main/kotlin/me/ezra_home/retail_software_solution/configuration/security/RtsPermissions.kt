package me.ezra_home.retail_software_solution.configuration.security

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.organizations.business.organization_admin.OrganizationAdminCache
import me.ezra_home.retail_software_solution.platform.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.business.PlatformAdmin
import org.springframework.stereotype.Service

@Service("rtsPermissions")
@TransactionalOnPlatformSchema
class RtsPermissions(private val organizationAdminCache: OrganizationAdminCache) {

    fun isPlatformAdmin(): Boolean {
        return PlatformAdmin.isPlatformAdmin()
    }

    fun isOrganizationAdmin(): Boolean {
        if (isPlatformAdmin()) return true
        return organizationAdminCache.getAdminHistory()
            .find { it.isActive() && it.userId == SessionContextProvider.getUserId() } != null
    }

}
