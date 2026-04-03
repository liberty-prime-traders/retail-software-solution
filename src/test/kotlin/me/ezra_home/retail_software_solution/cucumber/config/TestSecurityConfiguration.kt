package me.ezra_home.retail_software_solution.cucumber.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.configuration.filters.UserDataExtractionFilter
import me.ezra_home.retail_software_solution.configuration.security.OktaIdExtractor
import me.ezra_home.retail_software_solution.platform.business.sysuser.SysUserCache
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.orm.jpa.JpaTransactionManager
import me.ezra_home.retail_software_solution.configuration.security.RtsRoles
import me.ezra_home.retail_software_solution.cucumber.support.TestConstants
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.filter.OncePerRequestFilter

@TestConfiguration
class TestSecurityConfiguration {

  data class TestPrincipal(
    val token: String,
    val oktaId: String,
    val roles: List<String>
  )

  @Bean
  @Primary
  fun testSecurityFilterChain(
    http: HttpSecurity,
    testAuthenticationFilter: OncePerRequestFilter,
    userDataExtractionFilter: UserDataExtractionFilter
  ): SecurityFilterChain {
    return http.authorizeHttpRequests { it.requestMatchers("/secured/**").authenticated().anyRequest().permitAll() }
      .addFilterBefore(userDataExtractionFilter, BearerTokenAuthenticationFilter::class.java)
      .addFilterBefore(testAuthenticationFilter, UserDataExtractionFilter::class.java)
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
        val token = request.getHeader(TestConstants.Tokens.TOKEN_HEADER)
        val principal = mapTokenToPrincipal(token)
        if (principal != null) {
          SecurityContextHolder.getContext().authentication = mapPrincipalToAuthentication(principal)
        }
        filterChain.doFilter(request, response)
      }
    }
  }

  @Bean
  fun testOktaIdExtractor(): OktaIdExtractor = OktaIdExtractor { it.principal as String }

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
    val authorities = principal.roles.map { role -> SimpleGrantedAuthority("ROLE_$role") }
    return UsernamePasswordAuthenticationToken(principal.oktaId, principal.token, authorities)
  }
}
