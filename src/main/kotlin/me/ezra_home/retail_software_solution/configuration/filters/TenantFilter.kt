package me.ezra_home.retail_software_solution.configuration.filters

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.ezra_home.retail_software_solution.platform.session.SessionContextProvider
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class TenantFilter(
    private val locationSchemaInitializer: LocationSchemaInitializer,
    private val organizationSchemaInitializer: OrganizationSchemaInitializer
): OncePerRequestFilter() {

    override fun doFilterInternal(httpServletRequest: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        initializeSessionSchemaNames(httpServletRequest)
        SessionContextProvider.getSession().tenantFilterIsComplete = true
        try {
            chain.doFilter(httpServletRequest, response)
        } finally {
            SessionContextProvider.clear()
        }
    }

    private fun initializeSessionSchemaNames(httpServletRequest: HttpServletRequest) {
        organizationSchemaInitializer.initialize(httpServletRequest)
        locationSchemaInitializer.initialize(httpServletRequest)
    }

}
