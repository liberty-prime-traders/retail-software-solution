package me.ezra_home.retail_software_solution.configuration.filters

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.cross_tier.authority.EffectiveAuthorizationService
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class TenantFilter(
    private val locationSchemaInitializer: LocationSchemaInitializer,
    private val organizationSchemaInitializer: OrganizationSchemaInitializer,
    private val effectiveAuthorizationService: EffectiveAuthorizationService
): OncePerRequestFilter() {

    override fun doFilterInternal(httpServletRequest: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        initializeSessionSchemaNames(httpServletRequest)
        refreshEffectiveRoles()
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

    // Recomputes the effective role set now that org/location context (if any) is known, and
    // mirrors it onto the security context's authorities so existing hasRole(...) checks keep working.
    private fun refreshEffectiveRoles() {
        val systemUserId = SessionContextProvider.getUserIdOrNull() ?: return
        val currentAuthentication = SecurityContextHolder.getContext().authentication as? AbstractAuthenticationToken ?: return
        val jwt = currentAuthentication.principal as? Jwt ?: return

        val roles = effectiveAuthorizationService.getEffectiveRoles(systemUserId)
        val permissions = effectiveAuthorizationService.getEffectivePermissions(systemUserId)
        SessionContextProvider.getSession().roles = roles
        SessionContextProvider.getSession().permissions = permissions

        val authorities = roles.map { SimpleGrantedAuthority("ROLE_${it.name}") } +
            permissions.map { SimpleGrantedAuthority(it.name) }
        SecurityContextHolder.getContext().authentication = JwtAuthenticationToken(jwt, authorities)
    }
}
