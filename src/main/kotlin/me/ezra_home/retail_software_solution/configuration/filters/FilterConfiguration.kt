package me.ezra_home.retail_software_solution.configuration.filters

import me.ezra_home.retail_software_solution.platform.business.sysuser.SysUserCache
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FilterConfiguration {

    @Bean
    fun rtsSecureEndpointsFilter(sysUserCache: SysUserCache): FilterRegistrationBean<RtsSecureEndpointsFilter> {
        val registrationBean = FilterRegistrationBean(RtsSecureEndpointsFilter(sysUserCache))
        registrationBean.urlPatterns = listOf("/secured/*")
        registrationBean.setName("SessionContextInitializer")
        return registrationBean
    }
}
