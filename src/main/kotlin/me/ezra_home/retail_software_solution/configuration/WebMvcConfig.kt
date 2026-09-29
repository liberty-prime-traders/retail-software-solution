package me.ezra_home.retail_software_solution.configuration

import me.ezra_home.retail_software_solution.util.annotations.interceptors.FeatureGateInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val featureGateInterceptor: FeatureGateInterceptor
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(featureGateInterceptor)
    }
}
