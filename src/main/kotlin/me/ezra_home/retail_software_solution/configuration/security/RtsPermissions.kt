package me.ezra_home.retail_software_solution.configuration.security

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.locationadmin.LocationAdminCache
import me.ezra_home.retail_software_solution.platform.business.organizationadmin.OrganizationAdminCache
import me.ezra_home.retail_software_solution.platform.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.business.PlatformAdmin
import org.springframework.stereotype.Service

@Service("rtsPermissions")
@TransactionalOnPlatformSchema
class RtsPermissions(
    private val organizationAdminCache: OrganizationAdminCache,
    private val locationAdminCache: LocationAdminCache
) {

    fun isPlatformAdmin(): Boolean {
        return PlatformAdmin.isPlatformAdmin()
    }

    fun isOrganizationAdmin(): Boolean {
        if (isPlatformAdmin()) return true
        val organizationId = SessionContextProvider.getOrganizationId()
        return organizationAdminCache.getAdminHistoryForOrganization(organizationId)
            .find { it.isActive() && it.adminId == SessionContextProvider.getUserId() } != null
    }

    fun isLocationAdmin(): Boolean {
        if (isOrganizationAdmin()) return true
        val locationId = SessionContextProvider.getLocationId()
        return locationAdminCache.getAdminHistoryForLocation(locationId)
            .find { it.isActive() && it.adminId == SessionContextProvider.getUserId() } != null
    }

}
