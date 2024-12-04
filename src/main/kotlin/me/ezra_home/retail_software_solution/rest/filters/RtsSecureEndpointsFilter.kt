package me.ezra_home.retail_software_solution.rest.filters

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import me.ezra_home.retail_software_solution.rest.session.SessionContextProvider
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt


class RtsSecureEndpointsFilter(
    private val sessionContextProvider: SessionContextProvider
) : Filter {

    override fun doFilter(request: ServletRequest?, response: ServletResponse?, chain: FilterChain?) {
        val sessionContext = sessionContextProvider.getSession()

        val authentication = SecurityContextHolder.getContext().authentication
        sessionContext.oktaId = (authentication.principal as Jwt).subject

        chain?.doFilter(request, response)
    }

}
