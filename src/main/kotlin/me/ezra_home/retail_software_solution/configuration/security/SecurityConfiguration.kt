package me.ezra_home.retail_software_solution.configuration.security

import me.ezra_home.retail_software_solution.configuration.filters.UserDataExtractionFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@EnableWebSecurity
@Configuration
@Profile("!test")
class OktaOAuth2WebSecurityConfiguration(
    private val environment: Environment
) {

    @Bean
    @Throws(Exception::class)
    fun filterChain(http: HttpSecurity, userDataExtractionFilter: UserDataExtractionFilter): SecurityFilterChain {
        return http
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/secured/**").authenticated()
                    .anyRequest().permitAll()
            }
            .csrf { it.disable() }
            .cors(Customizer.withDefaults())
            .oauth2ResourceServer { it.jwt(Customizer.withDefaults()) }
            .addFilterAfter(userDataExtractionFilter, BearerTokenAuthenticationFilter::class.java)
            .build()
    }

    @Bean
    fun userDataExtractionFilterRegistration(filter: UserDataExtractionFilter): FilterRegistrationBean<UserDataExtractionFilter> {
        return FilterRegistrationBean(filter).apply { isEnabled = false }
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val originPatterns = environment.getProperty("CORS_PATTERNS")?.split(",") ?: listOf()
        val configuration = CorsConfiguration().apply {
            this.allowedOriginPatterns = originPatterns
            this.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
            this.allowedHeaders = listOf("*")
            this.allowCredentials = true
        }

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/secured/**", configuration)
        return source
    }
}
