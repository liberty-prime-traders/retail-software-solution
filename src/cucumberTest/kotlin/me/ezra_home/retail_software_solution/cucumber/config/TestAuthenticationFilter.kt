package me.ezra_home.retail_software_solution.cucumber.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.ezra_home.retail_software_solution.cucumber.support.TestUserRegistry
import me.ezra_home.retail_software_solution.support.TestConstants
import me.ezra_home.retail_software_solution.util.enums.RtsPermissionNames
import me.ezra_home.retail_software_solution.util.enums.RtsRoleNames
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Instant

class TestAuthenticationFilter : OncePerRequestFilter() {

  private data class TestPrincipal(
    val systemUserId: String,
    val roles: List<String> = emptyList(),
    val permissions: List<String> = emptyList()
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
      TestConstants.Tokens.PLATFORM_ADMIN -> TestUserRegistry.platformAdminUserId?.let {
        TestPrincipal(
          systemUserId = it.toString(),
          roles = listOf(RtsRoleNames.PLATFORM_ADMIN),
          permissions = listOf(RtsPermissionNames.CREATE_ORGANIZATION)
        )
      }
      TestConstants.Tokens.ORG_USER -> TestUserRegistry.organizationUserId?.let {
        TestPrincipal(systemUserId = it.toString(), roles = emptyList())
      }
      else -> null
    }
  }

  private fun mapPrincipalToAuthentication(principal: TestPrincipal): JwtAuthenticationToken {
    val authorities = principal.roles.map { SimpleGrantedAuthority("ROLE_$it") } +
      principal.permissions.map { SimpleGrantedAuthority(it) }
    val jwt = Jwt.withTokenValue(principal.systemUserId)
      .subject(principal.systemUserId)
      .header("alg", "none")
      .issuedAt(Instant.now())
      .expiresAt(Instant.now().plusSeconds(3600))
      .build()
    return JwtAuthenticationToken(jwt, authorities)
  }
}
