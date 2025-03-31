package me.ezra_home.retail_software_solution.platform.session

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
        return getSession().organizationId ?: throw RtsMissingHeaderException("Organization ID")
    }

    fun clear() {
        sessionContextThreadLocal.remove()
    }
}
