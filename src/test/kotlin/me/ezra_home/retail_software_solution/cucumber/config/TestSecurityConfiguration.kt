package me.ezra_home.retail_software_solution.cucumber.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.ezra_home.retail_software_solution.configuration.security.RtsHeaders
import me.ezra_home.retail_software_solution.configuration.security.RtsRoles
import me.ezra_home.retail_software_solution.configuration.session.LocationSession
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.cucumber.support.TestConstants
import me.ezra_home.retail_software_solution.cucumber.support.TestConstants.DEFAULT_ID
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

  @Bean
  @Primary
  fun testSecurityFilterChain(http: HttpSecurity, testAuthenticationFilter: OncePerRequestFilter): SecurityFilterChain {
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
          tenantFilterIsComplete = true

          this.location = LocationSession(
            id = request.getHeader(RtsHeaders.LOCATION_ID_HEADER)?.let { UUID.fromString(it) } ?: DEFAULT_ID,
            schemaName = TestConstants.Seed.LOCATION_SCHEMA,
            timezone = "UTC"
          )

          this.organization = OrgSession(
            id = request.getHeader(RtsHeaders.ORGANIZATION_ID_HEADER)?.let { UUID.fromString(it) } ?: DEFAULT_ID,
            schemaName = TestConstants.Seed.ORG_SCHEMA,
            timezone = "UTC"
          )
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
      TestConstants.Tokens.PLATFORM_ADMIN -> TestPrincipal(
        token = token,
        oktaId = "okta-platform-admin",
        systemUserId = DEFAULT_ID,
        roles = listOf(RtsRoles.ROLE_PLATFORM_ADMIN, RtsRoles.ROLE_CREATE_ORGANIZATION)
      )
      TestConstants.Tokens.ORG_USER -> TestPrincipal(
        token = token,
        oktaId = "okta-org-user",
        systemUserId = DEFAULT_ID,
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
