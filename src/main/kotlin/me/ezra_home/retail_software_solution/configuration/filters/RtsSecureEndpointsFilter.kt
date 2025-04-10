package me.ezra_home.retail_software_solution.configuration.filters

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.ezra_home.retail_software_solution.platform.business.sysuser.SysUserCache
import me.ezra_home.retail_software_solution.platform.session.SessionContext
import me.ezra_home.retail_software_solution.platform.session.SessionContextProvider
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.security.oauth2.jwt.Jwt


class RtsSecureEndpointsFilter(
    private val sysUserCache: SysUserCache,
    private val platformTransactionManager: JpaTransactionManager
) : OncePerRequestFilter() {

    companion object {
        private const val OKTA_ID_KEY = "uid"
    }

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        if (request.method.equals("OPTIONS", ignoreCase = true)) {
            chain.doFilter(request, response)
            return
        }
        val authentication = SecurityContextHolder.getContext().authentication
        val jwt = authentication.principal as Jwt

        val oktaIdForCurrentUser = jwt.claims[OKTA_ID_KEY] as String
        val sessionContext = SessionContextProvider.getSession()
        sessionContext.oktaId = oktaIdForCurrentUser
        initializeSystemUserId(sessionContext)

        try {
            chain.doFilter(request, response)
        } finally {
            SessionContextProvider.clear()
        }
    }

    private fun initializeSystemUserId(sessionContext: SessionContext) {
        val platformStatus = platformTransactionManager.getTransaction(null)
        val systemUserId = sysUserCache.getAllUsers().find { it.oktaId == sessionContext.oktaId }?.id
        sessionContext.systemUserId = systemUserId
        platformTransactionManager.commit(platformStatus)
    }
}
