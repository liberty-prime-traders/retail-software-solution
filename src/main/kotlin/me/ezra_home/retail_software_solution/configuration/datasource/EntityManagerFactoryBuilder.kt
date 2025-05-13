package me.ezra_home.retail_software_solution.configuration.datasource

import org.hibernate.context.spi.CurrentTenantIdentifierResolver
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import javax.sql.DataSource


object EntityManagerFactoryBuilder {

    fun build(dataSource: DataSource,
              connectionProvider: MultiTenantConnectionProvider<String>,
              packagesToScan: String,
              tenantIdentifierResolver: CurrentTenantIdentifierResolver<String>
    ): LocalContainerEntityManagerFactoryBean {

        return LocalContainerEntityManagerFactoryBean().apply {
            this.dataSource = dataSource
            this.setPackagesToScan(packagesToScan)
            this.jpaVendorAdapter = HibernateJpaVendorAdapter()
            this.setJpaPropertyMap(
                mapOf(
                    "hibernate.ddl.auto" to "none",
                    "hibernate.multiTenancy" to "SCHEMA",
                    "hibernate.tenant_identifier_resolver" to tenantIdentifierResolver,
                    "hibernate.multi_tenant_connection_provider" to connectionProvider
                )
            )
        }
    }
}
