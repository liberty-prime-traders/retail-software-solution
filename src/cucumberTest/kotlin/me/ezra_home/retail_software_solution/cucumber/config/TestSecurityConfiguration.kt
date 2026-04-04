package me.ezra_home.retail_software_solution.cucumber.config

import me.ezra_home.retail_software_solution.configuration.filters.UserDataExtractionFilter
import me.ezra_home.retail_software_solution.configuration.security.OktaIdExtractor
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.filter.OncePerRequestFilter

@TestConfiguration
class TestSecurityConfiguration {

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
  fun testAuthenticationFilter(): OncePerRequestFilter = TestAuthenticationFilter()

  @Bean
  fun testOktaIdExtractor(): OktaIdExtractor = OktaIdExtractor { it.principal as String }
}
