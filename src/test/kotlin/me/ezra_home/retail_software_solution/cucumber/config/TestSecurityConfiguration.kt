package me.ezra_home.retail_software_solution.cucumber.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@TestConfiguration
class TestSecurityConfiguration {

  @Bean
  @Primary
  fun testSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
    return http
      .authorizeHttpRequests { it.anyRequest().permitAll() }
      .csrf { it.disable() }
      .cors { it.disable() }
      .build()
  }

  @Bean
  fun testSessionFilter(): FilterRegistrationBean<*> {
    val filter = object : OncePerRequestFilter() {
      override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
      ) {
        val sessionContext = SessionContext().apply {
          systemUserId = UUID.fromString("00000000-0000-0000-0000-000000000001")
          oktaId = "test-okta-id"
          organizationId = UUID.fromString("00000000-0000-0000-0000-000000000001")
          organizationSchemaName = "public"
          locationId = UUID.fromString("00000000-0000-0000-0000-000000000001")
          locationSchemaName = "public"
          tenantFilterIsComplete = true
        }
        SessionContextProvider.setSession(sessionContext)
        try {
          chain.doFilter(request, response)
        } finally {
          SessionContextProvider.clear()
        }
      }
    }

    val registration = FilterRegistrationBean(filter)
    registration.order = Ordered.HIGHEST_PRECEDENCE
    registration.addUrlPatterns("/*")
    return registration
  }
}
