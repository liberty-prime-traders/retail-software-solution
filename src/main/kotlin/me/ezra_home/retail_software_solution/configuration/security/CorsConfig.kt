package me.ezra_home.retail_software_solution.configuration.security

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableWebMvc
class CorsConfig: WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        val originPatterns = System.getenv("CORS_PATTERNS").split(',').toTypedArray()
        registry.addMapping("/**").allowedOrigins(*originPatterns)
    }
}
