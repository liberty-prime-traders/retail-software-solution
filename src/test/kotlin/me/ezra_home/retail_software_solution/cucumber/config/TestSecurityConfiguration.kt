package me.ezra_home.retail_software_solution.cucumber.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.ezra_home.retail_software_solution.configuration.security.RtsHeaders
import me.ezra_home.retail_software_solution.configuration.security.RtsRoles
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@TestConfiguration
class TestSecurityConfiguration {

  data class TestPrincipal(
    val token: String,
    val oktaId: String,
    val systemUserId: UUID,
    val roles: List<String>
  )

  companion object {
    private val DEFAULT_USER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val DEFAULT_ORG_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val DEFAULT_LOCATION_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private const val DEFAULT_ORG_SCHEMA = "public"
    private const val DEFAULT_LOCATION_SCHEMA = "public"
  }

  @Bean
  @Primary
  fun testSecurityFilterChain(
    http: HttpSecurity,
    testAuthenticationFilter: OncePerRequestFilter
  ): SecurityFilterChain {
    return http
      .authorizeHttpRequests {
        it.requestMatchers("/secured/**").authenticated()
          .anyRequest().permitAll()
      }
      .addFilterBefore(testAuthenticationFilter, AnonymousAuthenticationFilter::class.java)
      .anonymous { it.disable() }
      .csrf { it.disable() }
      .cors { it.disable() }
      .build()
  }

  @Bean
  fun testAuthenticationFilter(): OncePerRequestFilter {
    return object : OncePerRequestFilter() {
      override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
      ) {
        val token = request.getHeader("Authorization")
          ?.removePrefix("Bearer ")
          ?.trim()
        val principal = mapTokenToPrincipal(token)
        if (principal != null) {
          SecurityContextHolder.getContext().authentication = mapPrincipalToAuthentication(principal)
        }
        filterChain.doFilter(request, response)
      }
    }
  }

  @Bean
  fun testSessionFilter(): FilterRegistrationBean<*> {
    val filter = object : OncePerRequestFilter() {
      override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
      ) {
        val token = request.getHeader("Authorization")
          ?.removePrefix("Bearer ")
          ?.trim()
        val principal = mapTokenToPrincipal(token)

        val sessionContext = SessionContext().apply {
          systemUserId = principal?.systemUserId
          oktaId = principal?.oktaId
          organizationId = request.getHeader(RtsHeaders.ORGANIZATION_ID_HEADER)?.let { UUID.fromString(it) } ?: DEFAULT_ORG_ID
          organizationSchemaName = DEFAULT_ORG_SCHEMA
          locationId = request.getHeader(RtsHeaders.LOCATION_ID_HEADER)?.let { UUID.fromString(it) } ?: DEFAULT_LOCATION_ID
          locationSchemaName = DEFAULT_LOCATION_SCHEMA
          tenantFilterIsComplete = true
        }
        SessionContextProvider.setSession(sessionContext)
        try {
          chain.doFilter(request, response)
        } finally {
          SecurityContextHolder.clearContext()
          SessionContextProvider.clear()
        }
      }
    }

    val registration = FilterRegistrationBean(filter)
    registration.order = Ordered.HIGHEST_PRECEDENCE
    registration.addUrlPatterns("/*")
    return registration
  }

  private fun mapTokenToPrincipal(token: String?): TestPrincipal? {
    if (token.isNullOrBlank() || token == "null") return null

    return when (token) {
      "mock-platform-admin-token" -> TestPrincipal(
        token = token,
        oktaId = "okta-platform-admin",
        systemUserId = DEFAULT_USER_ID,
        roles = listOf(RtsRoles.ROLE_PLATFORM_ADMIN, RtsRoles.ROLE_CREATE_ORGANIZATION)
      )
      "mock-org-admin-token" -> TestPrincipal(
        token = token,
        oktaId = "okta-org-admin",
        systemUserId = DEFAULT_USER_ID,
        roles = listOf(RtsRoles.ROLE_CREATE_ORGANIZATION)
      )
      "mock-user-token" -> TestPrincipal(
        token = token,
        oktaId = "okta-org-user",
        systemUserId = DEFAULT_USER_ID,
        roles = emptyList()
      )
      else -> null
    }
  }

  private fun mapPrincipalToAuthentication(principal: TestPrincipal): UsernamePasswordAuthenticationToken {
    val authorities = principal.roles.map { role -> SimpleGrantedAuthority("ROLE_$role") }
    return UsernamePasswordAuthenticationToken(principal.oktaId, principal.token, authorities)
  }
}
