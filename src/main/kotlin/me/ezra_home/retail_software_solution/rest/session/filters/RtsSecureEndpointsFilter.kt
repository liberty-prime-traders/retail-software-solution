package me.ezra_home.retail_software_solution.rest.session.filters

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import me.ezra_home.retail_software_solution.business.sysuser.SysUserCache
import me.ezra_home.retail_software_solution.rest.session.SessionContextProvider
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal
import java.util.Objects


class RtsSecureEndpointsFilter(
    private val sessionContextProvider: SessionContextProvider,
    private val sysUserCache: SysUserCache
) : Filter {

    companion object {
        private const val OKTA_ID_KEY = "uid"
    }
    override fun doFilter(request: ServletRequest?, response: ServletResponse?, chain: FilterChain?) {
        val authentication = SecurityContextHolder.getContext().authentication
        val jwt = authentication.principal as DefaultOAuth2AuthenticatedPrincipal
        val oktaIdForCurrentUser = jwt.attributes[OKTA_ID_KEY] as String
        val systemIdForCurrentUser = oktaIdForCurrentUser.let { oktaId ->
            sysUserCache.getAllUsers().find { Objects.equals(oktaId, it.oktaId) }?.id
        }

        val sessionContext = sessionContextProvider.getSession()
        sessionContext.oktaId = oktaIdForCurrentUser
        sessionContext.systemUserId = systemIdForCurrentUser

        chain?.doFilter(request, response)
    }

}
