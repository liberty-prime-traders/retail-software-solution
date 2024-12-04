package me.ezra_home.retail_software_solution.rest.filters

import me.ezra_home.retail_software_solution.rest.session.SessionContextProvider
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FilterConfiguration {

    @Bean
    fun rtsSecureEndpointsFilter(sessionContextProvider: SessionContextProvider): FilterRegistrationBean<RtsSecureEndpointsFilter> {
        val registrationBean = FilterRegistrationBean(RtsSecureEndpointsFilter(sessionContextProvider))
        registrationBean.urlPatterns = listOf("/secured/*")
        registrationBean.setName("SessionContextInitializer")
        return registrationBean
    }
}
