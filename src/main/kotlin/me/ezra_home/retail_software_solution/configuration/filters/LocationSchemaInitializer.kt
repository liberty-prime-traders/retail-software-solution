package me.ezra_home.retail_software_solution.configuration.filters

import jakarta.servlet.http.HttpServletRequest
import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.configuration.security.RtsHeaders.LOCATION_ID_HEADER
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationService
import me.ezra_home.retail_software_solution.util.business.StringUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.stereotype.Component
import org.springframework.transaction.support.DefaultTransactionDefinition
import java.util.UUID

@Component
class LocationSchemaInitializer(
    private val locationService: LocationService,
    @param:Qualifier(DataSourceBeanNames.ORGANIZATION_SCHEMA_TRANSACTION_MANAGER)
    private val organizationTransactionManager: JpaTransactionManager
) {
    fun initialize(httpServletRequest: HttpServletRequest) {
        if (SessionContextProvider.getSession().organization == null) return
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
            ?.let { UUID.fromString(it) }
            ?.let { locationId -> locationService.getAllLocationDtos().find { it.id == locationId } }
            ?.let { SessionContextProvider.initLocation(it) }
    }
}
