package me.ezra_home.retail_software_solution.configuration.filters

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.configuration.security.RtsHeaders.LOCATION_ID_HEADER
import me.ezra_home.retail_software_solution.configuration.security.RtsHeaders.ORGANIZATION_ID_HEADER
import me.ezra_home.retail_software_solution.platform.business.location.LocationCache
import me.ezra_home.retail_software_solution.platform.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.business.StringUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.stereotype.Component
import org.springframework.transaction.support.DefaultTransactionDefinition
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class TenantFilter(
    private val locationCache: LocationCache,
    @Qualifier(DataSourceBeanNames.PLATFORM_SCHEMA_TRANSACTION_MANAGER)
    private val platformTransactionManager: JpaTransactionManager
): OncePerRequestFilter() {

    override fun doFilterInternal(httpServletRequest: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val sessionContext = SessionContextProvider.getSession()
        sessionContext.organizationId = httpServletRequest.getHeader(ORGANIZATION_ID_HEADER)
            ?.takeIf { StringUtils.hasValue(it) }
            ?.let { UUID.fromString(it) }
        initializeSessionSchemaName(httpServletRequest)
        try {
            chain.doFilter(httpServletRequest, response)
        } finally {
            SessionContextProvider.clear()
        }
    }

    private fun initializeSessionSchemaName(httpServletRequest: HttpServletRequest) {
        val platformStatus = platformTransactionManager.getTransaction(DefaultTransactionDefinition())
        httpServletRequest.getHeader(LOCATION_ID_HEADER)
            ?.takeIf { StringUtils.hasValue(it) }
            ?.let {
                val locationId = UUID.fromString(it)
                SessionContextProvider.getSession().locationId = locationId
                locationId
            }
            ?.let { locId -> locationCache.getAllLocations().find { it.id == locId }?.schemaName }
            ?.let { SessionContextProvider.getSession().schemaName = it }
        platformTransactionManager.commit(platformStatus)
        SessionContextProvider.getSession().filteredForLocation = true
    }
}
