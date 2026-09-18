package me.ezra_home.retail_software_solution.configuration.filters

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.SysUserService
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.hibernate.annotations.Filter
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
@Filter(name = "UserDataExtractionFilter")
@Order(Ordered.LOWEST_PRECEDENCE)
class UserDataExtractionFilter(private val sysUserService: SysUserService) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        if (request.method.equals("OPTIONS", ignoreCase = true)) {
            chain.doFilter(request, response)
            return
        }

        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication != null) {
            try {
                val systemUserId = UUID.fromString((authentication.principal as Jwt).subject)
                sysUserService.throwIfAccountIsDisabled(systemUserId)
                SessionContextProvider.getSession().systemUserId = systemUserId
            } catch (_: ClassCastException) {
                throw RtsGenericException("Failed to extract user data from security context")
            }
        }

        try {
            chain.doFilter(request, response)
        } finally {
            SessionContextProvider.clear()
        }
    }
}
