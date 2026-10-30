package me.ezra_home.retail_software_solution.organizations.business.liberty_access.api

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.enums.RtsPermission
import org.springframework.stereotype.Service

@Service
class LibertyAccessService {

    fun getLibertyPermissionsOfCurrentUser(): List<RtsPermission> =
        SessionContextProvider.getSession().permissions
            .filter { it in LIBERTY_PERMISSIONS }

    companion object {
        private val LIBERTY_PERMISSIONS = setOf(
            RtsPermission.LIBERTY_ADMIN,
            RtsPermission.MANAGE_ORGANIZATION_ACCESS
        )
    }
}
