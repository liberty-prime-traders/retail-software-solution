package me.ezra_home.retail_software_solution.platform.business.organizationadmin

import me.ezra_home.retail_software_solution.platform.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.business.PlatformAdmin
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OrganizationAdminValidator(private val organizationAdminCache: OrganizationAdminCache) {

    fun canCurrentUserOperateOnOrganization(organizationId: UUID?) {
        if (PlatformAdmin.isPlatformAdmin() || organizationId == null) return
        organizationAdminCache.getAdminHistoryForOrganization(organizationId)
            .find { it.endOn == null && it.adminId == SessionContextProvider.getSession().systemUserId }
            ?: throw RtsGenericException("User is not authorized to update organization")
    }
}
