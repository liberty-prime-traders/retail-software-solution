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
import javax.sql.DataSource

@Configuration
@EnableJpaRepositories(
    basePackages = ["me.ezra_home.retail_software_solution.organizations"],
    entityManagerFactoryRef = DataSourceBeanNames.ORGANIZATION_SCHEMA_ENTITY_MANAGER_FACTORY,
    transactionManagerRef = DataSourceBeanNames.ORGANIZATION_SCHEMA_TRANSACTION_MANAGER
)
class OrganizationSchemaDataSourceConfig(private val organizationTenantIdentifier: OrganizationTenantIdentifier) {

    @Primary
    @Bean(name = [DataSourceBeanNames.ORGANIZATION_SCHEMA_DATA_SOURCE])
    @ConfigurationProperties(prefix = "spring.datasource.organization")
    fun organizationSchemaDataSource(): DataSource = DataSourceBuilder.create().build()

    @Bean(name = [DataSourceBeanNames.ORGANIZATION_SCHEMA_ENTITY_MANAGER_FACTORY])
    fun organizationSchemaEntityManagerFactory(
        @Qualifier(DataSourceBeanNames.ORGANIZATION_SCHEMA_DATA_SOURCE) dataSource: DataSource,
        @Qualifier(DataSourceBeanNames.ORGANIZATION_SCHEMA_CONNECTION_PROVIDER) connectionProvider: MultiTenantConnectionProvider<String>
    ): LocalContainerEntityManagerFactoryBean {
        val packagesToScan = "me.ezra_home.retail_software_solution.organizations"
        return EntityManagerFactoryBuilder.build(dataSource, connectionProvider, packagesToScan, organizationTenantIdentifier)
    }

    @Bean(name = [DataSourceBeanNames.ORGANIZATION_SCHEMA_TRANSACTION_MANAGER])
    fun organizationSchemaTransactionManager(
        @Qualifier(DataSourceBeanNames.ORGANIZATION_SCHEMA_ENTITY_MANAGER_FACTORY) emf: LocalContainerEntityManagerFactoryBean
    ): JpaTransactionManager {
        return JpaTransactionManager().apply { entityManagerFactory = emf.getObject() }
    }

    @Bean(name = [DataSourceBeanNames.ORGANIZATION_SCHEMA_LIQUIBASE])
    fun organizationSchemaLiquibase(
        @Qualifier(DataSourceBeanNames.ORGANIZATION_SCHEMA_DATA_SOURCE) dataSource: DataSource
    ): SpringLiquibase {
        return SpringLiquibase().apply {
            this.dataSource = dataSource
            changeLog = "classpath:db/changelog/organizations/db-changelog-master.yml"
            setShouldRun(false)
        }
    }
}
