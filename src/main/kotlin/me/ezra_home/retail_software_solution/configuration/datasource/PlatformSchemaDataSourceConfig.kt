package me.ezra_home.retail_software_solution.configuration.datasource

import liquibase.integration.spring.SpringLiquibase
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import javax.sql.DataSource

@Configuration
@EnableJpaRepositories(
    basePackages = ["me.ezra_home.retail_software_solution.platform"],
    entityManagerFactoryRef = DataSourceBeanNames.PLATFORM_SCHEMA_ENTITY_MANAGER_FACTORY,
    transactionManagerRef = DataSourceBeanNames.PLATFORM_SCHEMA_TRANSACTION_MANAGER
)
class PlatformSchemaDataSourceConfig {

    @Bean(name = [DataSourceBeanNames.PLATFORM_SCHEMA_DATA_SOURCE])
    @ConfigurationProperties(prefix = "spring.datasource.platform")
    fun platformSchemaDataSource(): DataSource = DataSourceBuilder.create().build()

    @Bean(name = [DataSourceBeanNames.PLATFORM_SCHEMA_ENTITY_MANAGER_FACTORY])
    fun platformSchemaEntityManagerFactory(
        @Qualifier(DataSourceBeanNames.PLATFORM_SCHEMA_DATA_SOURCE) dataSource: DataSource
    ): LocalContainerEntityManagerFactoryBean {
        val em = LocalContainerEntityManagerFactoryBean()
        em.dataSource = dataSource
        em.setPackagesToScan("me.ezra_home.retail_software_solution.platform.model")
        em.jpaVendorAdapter = HibernateJpaVendorAdapter()
        em.setJpaPropertyMap(mapOf("hibernate.ddl.auto" to "none"))
        return em
    }

    @Bean(name = [DataSourceBeanNames.PLATFORM_SCHEMA_TRANSACTION_MANAGER])
    fun platformSchemaTransactionManager(
        @Qualifier(DataSourceBeanNames.PLATFORM_SCHEMA_ENTITY_MANAGER_FACTORY) emf: LocalContainerEntityManagerFactoryBean
    ): JpaTransactionManager {
        return JpaTransactionManager().apply { entityManagerFactory = emf.getObject() }
    }

    @Bean(name = [DataSourceBeanNames.PLATFORM_SCHEMA_LIQUIBASE])
    fun platformSchemaLiquibase(
        @Qualifier(DataSourceBeanNames.PLATFORM_SCHEMA_DATA_SOURCE) dataSource: DataSource
    ): SpringLiquibase {
        val liquibase = SpringLiquibase()
        liquibase.dataSource = dataSource
        liquibase.changeLog = "classpath:db/changelog/platform/db-changelog-master.yml"
        liquibase.defaultSchema = "platform"
        liquibase.setShouldRun(true)
        return liquibase
    }
}
