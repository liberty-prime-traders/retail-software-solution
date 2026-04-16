package me.ezra_home.retail_software_solution.configuration.session

import me.ezra_home.retail_software_solution.configuration.security.RtsHeaders
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationDto
import me.ezra_home.retail_software_solution.platform.business.organization.api.OrganizationDto
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.RtsMissingHeaderException
import java.util.UUID


object SessionContextProvider {
    private val sessionContextThreadLocal = ThreadLocal<SessionContext?>()

    fun setSession(sessionContext: SessionContext) {
        sessionContextThreadLocal.set(sessionContext)
    }

    fun getSession(): SessionContext {
        return sessionContextThreadLocal.get() ?: SessionContext().also { sessionContextThreadLocal.set(it) }
    }

    fun getUserId(): UUID {
        return getSession().systemUserId ?: throw RtsGenericException("User ID not found in session")
    }

    fun getOrganizationId(): UUID {
        return getSession().organization?.id ?: throw RtsMissingHeaderException(RtsHeaders.ORGANIZATION_ID_HEADER)
    }

    fun getLocationId(): UUID {
        return getSession().location?.id ?: throw RtsMissingHeaderException(RtsHeaders.LOCATION_ID_HEADER)
    }

    fun getLocationSchema(): String {
        return getSession().location?.schemaName ?: throw RtsGenericException("Location schema not found in session.")
    }

    fun getOrganizationSchema(): String {
        return getSession().organization?.schemaName ?: throw RtsGenericException("Organization schema not found in session.")
    }

    fun getOrgTimezone(): String {
        return getSession().organization?.timezone ?: throw RtsGenericException("Organization timezone not found in session.")
    }

    fun clear() {
        sessionContextThreadLocal.remove()
    }

    fun initOrganization(organization: OrganizationDto) {
        getSession().organization = OrgSession(
            id = organization.id,
            schemaName = organization.schemaName!!,
            timezone = organization.timezone
        )
    }

    fun initLocation(location: LocationDto) {
        getSession().location = LocationSession(
            id = location.id,
            schemaName = location.schemaName!!,
            timezone = location.timezone
        )
    }

    fun initSystemUser(userId: UUID) {
        getSession().systemUserId = userId
    }
}
