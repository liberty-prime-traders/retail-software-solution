package me.ezra_home.retail_software_solution.configuration.filters

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.ezra_home.retail_software_solution.configuration.security.RtsHeaders.ORGANIZATION_ID_HEADER
import me.ezra_home.retail_software_solution.platform.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.business.StringUtils
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class TenantFilter(
    private val locationSchemaInitializer: LocationSchemaInitializer,
    private val organizationSchemaInitializer: OrganizationSchemaInitializer
): OncePerRequestFilter() {

    override fun doFilterInternal(httpServletRequest: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val sessionContext = SessionContextProvider.getSession()
        sessionContext.organizationId = httpServletRequest.getHeader(ORGANIZATION_ID_HEADER)
            ?.takeIf { StringUtils.hasValue(it) }
            ?.let { UUID.fromString(it) }
        initializeSessionSchemaNames(httpServletRequest)
        SessionContextProvider.getSession().tenantFilterIsComplete = true
        try {
            chain.doFilter(httpServletRequest, response)
        } finally {
            SessionContextProvider.clear()
        }
    }

    private fun initializeSessionSchemaNames(httpServletRequest: HttpServletRequest) {
        locationSchemaInitializer.initialize(httpServletRequest)
        organizationSchemaInitializer.initialize(httpServletRequest)
    }

}
