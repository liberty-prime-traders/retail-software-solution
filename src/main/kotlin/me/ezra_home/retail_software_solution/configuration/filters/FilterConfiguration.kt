package me.ezra_home.retail_software_solution.configuration.filters

import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.platform.business.sysuser.SysUserCache
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.orm.jpa.JpaTransactionManager

@Configuration
class FilterConfiguration {

    @Bean
    fun rtsSecureEndpointsFilter(sysUserCache: SysUserCache,
                                 @Qualifier(DataSourceBeanNames.PLATFORM_SCHEMA_TRANSACTION_MANAGER)
                                 platformTransactionManager: JpaTransactionManager
    ): FilterRegistrationBean<RtsSecureEndpointsFilter> {
        val registrationBean = FilterRegistrationBean(RtsSecureEndpointsFilter(sysUserCache, platformTransactionManager))
        registrationBean.urlPatterns = listOf("/secured/*")
        registrationBean.setName("SessionContextInitializer")
        return registrationBean
    }
}
