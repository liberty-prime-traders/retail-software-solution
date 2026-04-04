package me.ezra_home.retail_software_solution.cucumber.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.ezra_home.retail_software_solution.configuration.security.RtsRoles
import me.ezra_home.retail_software_solution.support.TestConstants
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class TestAuthenticationFilter : OncePerRequestFilter() {

  private data class TestPrincipal(
    val token: String,
    val oktaId: String,
    val roles: List<String>
  )

  override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
    val token = request.getHeader(TestConstants.Tokens.TOKEN_HEADER)
    val principal = mapTokenToPrincipal(token)
    if (principal != null) {
      SecurityContextHolder.getContext().authentication = mapPrincipalToAuthentication(principal)
    }
    filterChain.doFilter(request, response)
  }

  private fun mapTokenToPrincipal(token: String?): TestPrincipal? {
    if (token.isNullOrBlank() || token == "null") return null
    return when (token) {
      TestConstants.Tokens.PLATFORM_ADMIN -> TestPrincipal(
        token = token,
        oktaId = TestConstants.Okta.PLATFORM_USER,
        roles = listOf(RtsRoles.ROLE_PLATFORM_ADMIN, RtsRoles.ROLE_CREATE_ORGANIZATION)
      )
      TestConstants.Tokens.ORG_USER -> TestPrincipal(
        token = token,
        oktaId = TestConstants.Okta.ORGANIZATION_USER,
        roles = emptyList()
      )
      else -> null
    }
  }

  private fun mapPrincipalToAuthentication(principal: TestPrincipal): UsernamePasswordAuthenticationToken {
    val authorities = principal.roles.map { SimpleGrantedAuthority("ROLE_$it") }
    return UsernamePasswordAuthenticationToken(principal.oktaId, principal.token, authorities)
  }
}
