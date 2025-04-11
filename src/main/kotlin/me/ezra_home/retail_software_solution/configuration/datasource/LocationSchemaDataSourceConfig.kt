package me.ezra_home.retail_software_solution.configuration.datasource

import liquibase.integration.spring.SpringLiquibase
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import javax.sql.DataSource

@Configuration
@EnableJpaRepositories(
    basePackages = ["me.ezra_home.retail_software_solution.locations"],
    entityManagerFactoryRef = DataSourceBeanNames.LOCATION_SCHEMA_ENTITY_MANAGER_FACTORY,
    transactionManagerRef = DataSourceBeanNames.LOCATION_SCHEMA_TRANSACTION_MANAGER
)
class LocationSchemaDataSourceConfig {

    @Bean(name = [DataSourceBeanNames.LOCATION_SCHEMA_DATA_SOURCE])
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.location")
    fun locationSchemaDataSource(): DataSource = DataSourceBuilder.create().build()

    @Bean(name = [DataSourceBeanNames.LOCATION_SCHEMA_ENTITY_MANAGER_FACTORY])
    fun locationSchemaEntityManagerFactory(
        @Qualifier(DataSourceBeanNames.LOCATION_SCHEMA_DATA_SOURCE) dataSource: DataSource,
        @Qualifier(DataSourceBeanNames.LOCATION_SCHEMA_CONNECTION_PROVIDER) connectionProvider: MultiTenantConnectionProvider<String>
    ): LocalContainerEntityManagerFactoryBean {
        val em = LocalContainerEntityManagerFactoryBean()
        val dataSourcePackage = "me.ezra_home.retail_software_solution.configuration.datasource"
        em.dataSource = dataSource
        em.setPackagesToScan("me.ezra_home.retail_software_solution.locations")
        em.jpaVendorAdapter = HibernateJpaVendorAdapter()
        em.setJpaPropertyMap(mapOf(
            "hibernate.ddl.auto" to "none",
            "hibernate.multiTenancy" to "SCHEMA",
            "hibernate.tenant_identifier_resolver" to "$dataSourcePackage.TenantIdentifierResolver",
            "hibernate.multi_tenant_connection_provider" to connectionProvider
        ))
        return em
    }

    @Bean(name = [DataSourceBeanNames.LOCATION_SCHEMA_TRANSACTION_MANAGER])
    fun locationSchemaTransactionManager(
        @Qualifier(DataSourceBeanNames.LOCATION_SCHEMA_ENTITY_MANAGER_FACTORY) emf: LocalContainerEntityManagerFactoryBean
    ): JpaTransactionManager {
        return JpaTransactionManager().apply { entityManagerFactory = emf.getObject() }
    }

    @Bean(name = [DataSourceBeanNames.LOCATION_SCHEMA_LIQUIBASE])
    fun locationSchemaLiquibase(
        @Qualifier(DataSourceBeanNames.LOCATION_SCHEMA_DATA_SOURCE) dataSource: DataSource
    ): SpringLiquibase {
        return SpringLiquibase().apply {
            this.dataSource = dataSource
            changeLog = "classpath:db/changelog/platform/db-changelog-master.yml"
            setShouldRun(false)
        }
    }
}
