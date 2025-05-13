package me.ezra_home.retail_software_solution.configuration.datasource

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class TenantConnectionProviders {

    @Bean(value = [DataSourceBeanNames.ORGANIZATION_SCHEMA_CONNECTION_PROVIDER])
    fun organizationConnectionProvider(
        @Qualifier(DataSourceBeanNames.ORGANIZATION_SCHEMA_DATA_SOURCE)
        dataSource: DataSource
    ): RtsMultiTenantConnectionProvider {

        return RtsMultiTenantConnectionProvider(dataSource)
    }

    @Bean(value = [DataSourceBeanNames.LOCATION_SCHEMA_CONNECTION_PROVIDER])
    fun locationConnectionProvider(
        @Qualifier(DataSourceBeanNames.LOCATION_SCHEMA_DATA_SOURCE)
        dataSource: DataSource
    ): RtsMultiTenantConnectionProvider {

        return RtsMultiTenantConnectionProvider(dataSource)
    }
}
