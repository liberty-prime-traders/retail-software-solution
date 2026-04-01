package me.ezra_home.retail_software_solution.configuration.session

import me.ezra_home.retail_software_solution.configuration.security.RtsHeaders
import me.ezra_home.retail_software_solution.organizations.model.LocationEntity
import me.ezra_home.retail_software_solution.platform.model.OrganizationEntity
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

    fun getOrgTimezone(): String {
        return getSession().organization?.timezone ?: throw RtsGenericException("Organization timezone not found in session.")
    }

    fun clear() {
        sessionContextThreadLocal.remove()
    }

    fun initOrganization(organization: OrganizationEntity) {
        getSession().organization = OrgSession(
            id = organization.getNullSafeId(),
            schemaName = organization.schemaName!!,
            timezone = organization.timezone
        )
    }

    fun initLocation(location: LocationEntity) {
        getSession().location = LocationSession(
            id = location.getNullSafeId(),
            schemaName = location.schemaName!!,
            timezone = location.timezone
        )
    }

    fun initSystemUser(userId: UUID) {
        getSession().systemUserId = userId
    }
}
