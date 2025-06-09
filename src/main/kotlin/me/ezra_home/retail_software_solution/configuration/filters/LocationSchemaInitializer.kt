package me.ezra_home.retail_software_solution.configuration.filters

import jakarta.servlet.http.HttpServletRequest
import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.configuration.security.RtsHeaders.LOCATION_ID_HEADER
import me.ezra_home.retail_software_solution.organizations.business.location.LocationCache
import me.ezra_home.retail_software_solution.platform.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.business.StringUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.stereotype.Component
import org.springframework.transaction.support.DefaultTransactionDefinition
import java.util.UUID

@Component
class LocationSchemaInitializer(
    private val locationCache: LocationCache,
    @Qualifier(DataSourceBeanNames.ORGANIZATION_SCHEMA_TRANSACTION_MANAGER)
    private val organizationTransactionManager: JpaTransactionManager
) {
    fun initialize(httpServletRequest: HttpServletRequest) {
        if (!StringUtils.hasValue(SessionContextProvider.getSession().organizationSchemaName)) {
            // If the organization schema name is not set, skip initialization
            return
        }
        val organizationStatus = organizationTransactionManager.getTransaction(DefaultTransactionDefinition())
        try {
            doInitialize(httpServletRequest)
        } finally {
            organizationTransactionManager.commit(organizationStatus)
        }
    }

    private fun doInitialize(httpServletRequest: HttpServletRequest) {
        httpServletRequest.getHeader(LOCATION_ID_HEADER)
            ?.takeIf { StringUtils.hasValue(it) }
            ?.let {
                val locationId = UUID.fromString(it)
                SessionContextProvider.getSession().locationId = locationId
                locationId
            }
            ?.let { locationId -> locationCache.getAllLocations().find { it.id == locationId }?.schemaName }
            ?.let { SessionContextProvider.getSession().locationSchemaName = it }
    }
}
