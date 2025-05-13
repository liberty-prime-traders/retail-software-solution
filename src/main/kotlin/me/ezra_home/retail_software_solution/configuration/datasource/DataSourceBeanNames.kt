package me.ezra_home.retail_software_solution.configuration.datasource

object DataSourceBeanNames {
    const val LOCATION_SCHEMA_ENTITY_MANAGER_FACTORY = "locationSchemaEntityManagerFactory"
    const val LOCATION_SCHEMA_TRANSACTION_MANAGER = "locationSchemaTransactionManager"
    const val LOCATION_SCHEMA_DATA_SOURCE = "locationSchemaDataSource"
    const val LOCATION_SCHEMA_CONNECTION_PROVIDER = "LocationSchemaConnectionProvider"
    const val LOCATION_SCHEMA_LIQUIBASE = "locationSchemaLiquibase"

    const val PLATFORM_SCHEMA_ENTITY_MANAGER_FACTORY = "platformSchemaEntityManagerFactory"
    const val PLATFORM_SCHEMA_TRANSACTION_MANAGER = "platformSchemaTransactionManager"
    const val PLATFORM_SCHEMA_DATA_SOURCE = "platformSchemaDataSource"
    const val PLATFORM_SCHEMA_LIQUIBASE = "platformSchemaLiquibase"

    const val ORGANIZATION_SCHEMA_ENTITY_MANAGER_FACTORY = "organizationSchemaEntityManagerFactory"
    const val ORGANIZATION_SCHEMA_TRANSACTION_MANAGER = "organizationSchemaTransactionManager"
    const val ORGANIZATION_SCHEMA_DATA_SOURCE = "organizationSchemaDataSource"
    const val ORGANIZATION_SCHEMA_CONNECTION_PROVIDER = "OrganizationSchemaConnectionProvider"
    const val ORGANIZATION_SCHEMA_LIQUIBASE = "organizationSchemaLiquibase"
}
