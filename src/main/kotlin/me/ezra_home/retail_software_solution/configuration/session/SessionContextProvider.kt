package me.ezra_home.retail_software_solution.configuration.session

import me.ezra_home.retail_software_solution.configuration.security.RtsHeaders
import me.ezra_home.retail_software_solution.platform.model.OrganizationEntity
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.RtsMissingHeaderException
import java.util.UUID


object SessionContextProvider {
    private val sessionContextThreadLocal = ThreadLocal<SessionContext?>()

    fun getSession(): SessionContext {
        return sessionContextThreadLocal.get() ?: SessionContext().also { sessionContextThreadLocal.set(it) }
    }

    fun getUserId(): UUID  {
        return getSession().systemUserId ?: throw RtsGenericException("User ID not found in session")
    }

    fun getOrganizationId(): UUID {
        return getSession().organizationId ?: throw RtsMissingHeaderException(RtsHeaders.ORGANIZATION_ID_HEADER)
    }

    fun getLocationId(): UUID {
        return getSession().locationId ?: throw RtsMissingHeaderException(RtsHeaders.LOCATION_ID_HEADER)
    }

    fun clear() {
        sessionContextThreadLocal.remove()
    }

    fun initOrganization(organization: OrganizationEntity) {
        getSession().organizationId = organization.id
        getSession().organizationSchemaName = organization.schemaName
    }
}
