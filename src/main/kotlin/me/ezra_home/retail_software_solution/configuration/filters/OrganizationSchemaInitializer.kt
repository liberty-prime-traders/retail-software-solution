package me.ezra_home.retail_software_solution.configuration.filters

import jakarta.servlet.http.HttpServletRequest
import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.configuration.security.RtsHeaders.ORGANIZATION_ID_HEADER
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.platform.business.organization.api.OrganizationService
import me.ezra_home.retail_software_solution.util.business.StringUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.stereotype.Component
import org.springframework.transaction.support.DefaultTransactionDefinition
import java.util.UUID

@Component
class OrganizationSchemaInitializer(
    private val organizationService: OrganizationService,
    @param:Qualifier(DataSourceBeanNames.PLATFORM_SCHEMA_TRANSACTION_MANAGER)
    private val platformTransactionManager: JpaTransactionManager
) {

    fun initialize(httpServletRequest: HttpServletRequest) {
        val platformStatus = platformTransactionManager.getTransaction(DefaultTransactionDefinition())
        try {
            doInitialize(httpServletRequest)
        } finally {
            platformTransactionManager.commit(platformStatus)
        }
    }

    private fun doInitialize(httpServletRequest: HttpServletRequest) {
        httpServletRequest.getHeader(ORGANIZATION_ID_HEADER)
            ?.takeIf { StringUtils.hasValue(it) }
            ?.let { UUID.fromString(it) }
            ?.let { organizationId -> organizationService.getAllOrganizationDtos().find { it.id == organizationId } }
            ?.let { SessionContextProvider.initOrganization(it) }
    }
}
