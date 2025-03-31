package me.ezra_home.retail_software_solution.configuration.security

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.organizationadmin.OrganizationAdminCache
import me.ezra_home.retail_software_solution.platform.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.business.PlatformAdmin
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

@Service("rtsPermissions")
@TransactionalOnPlatformSchema
class RtsPermissions(private val organizationAdminCache: OrganizationAdminCache) {

    fun operateOnOrganization() {
        if (PlatformAdmin.isPlatformAdmin()) return
        val organizationId = SessionContextProvider.getOrganizationId()
        organizationAdminCache.getAdminHistoryForOrganization(organizationId)
            .find { it.isActive() && it.adminId == SessionContextProvider.getUserId() }
            ?: throw RtsGenericException("User is not authorized to operate on organization")
    }
}
