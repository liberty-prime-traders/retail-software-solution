package me.ezra_home.retail_software_solution.util.business

import me.ezra_home.retail_software_solution.configuration.security.RtsRoles
import org.springframework.security.core.context.SecurityContextHolder

internal object PlatformAdmin {

    fun isPlatformAdmin(): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication.authorities.any { it.authority == "ROLE_${RtsRoles.ROLE_PLATFORM_ADMIN}"}
    }
}
