package me.ezra_home.retail_software_solution.configuration.filters

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.platform.business.sysuser.SysUserCache
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.hibernate.annotations.Filter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter


@Component
@Filter(name = "UserDataExtractionFilter")
class UserDataExtractionFilter(
    private val sysUserCache: SysUserCache,
    @param:Qualifier(DataSourceBeanNames.PLATFORM_SCHEMA_TRANSACTION_MANAGER)
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

        try {
            val authentication = SecurityContextHolder.getContext().authentication
            val jwt = authentication.principal as Jwt
            val sessionContext = SessionContextProvider.getSession()
            sessionContext.oktaId = jwt.claims[OKTA_ID_KEY] as String
            initializeSystemUserId(sessionContext)
        } catch (_: ClassCastException) {
            throw RtsGenericException("Failed to extract user data from security context")
        }

        try {
            chain.doFilter(request, response)
        } finally {
            SessionContextProvider.clear()
        }
    }

    private fun initializeSystemUserId(sessionContext: SessionContext) {
        val platformStatus = platformTransactionManager.getTransaction(null)
        sysUserCache.getAllUsers().find { it.oktaId == sessionContext.oktaId }
            ?.let { sessionContext.systemUserId = it.id }
        platformTransactionManager.commit(platformStatus)
    }
}
