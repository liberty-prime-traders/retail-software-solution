package me.ezra_home.retail_software_solution.rest.session

import org.springframework.stereotype.Component

@Component
class SessionContextProvider {
    private val sessionContextThreadLocal = ThreadLocal<SessionContext?>()

    fun getSession(): SessionContext {
        val sessionContext = sessionContextThreadLocal.get()
        if (sessionContext == null) {
            val newSessionContext = SessionContext()
            sessionContextThreadLocal.set(newSessionContext)
            return newSessionContext
        } else {
            return sessionContext
        }
    }

    fun clear() {
        sessionContextThreadLocal.remove()
    }

    fun sessionContextExists(): Boolean {
        return sessionContextThreadLocal.get() != null
    }
}
