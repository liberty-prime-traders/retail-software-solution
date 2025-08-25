package me.ezra_home.retail_software_solution.configuration.datasource

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.hibernate.context.spi.CurrentTenantIdentifierResolver
import org.springframework.stereotype.Component

@Component
class OrganizationTenantIdentifier: CurrentTenantIdentifierResolver<String> {

    override fun resolveCurrentTenantIdentifier(): String {
        val schemaName = SessionContextProvider.getSession().organizationSchemaName
        if (schemaName.isNullOrBlank() && SessionContextProvider.getSession().tenantFilterIsComplete) {
            throw RtsGenericException("Organization schema name is not set in the session context")
        }
        // The default schema is used during the application startup when the session context is not yet available
        // This schema does not exist in the database, but it is used to satisfy the Hibernate's metadata validation
        return schemaName ?: "placeholder_schema"
    }

    override fun validateExistingCurrentSessions(): Boolean = true
}
