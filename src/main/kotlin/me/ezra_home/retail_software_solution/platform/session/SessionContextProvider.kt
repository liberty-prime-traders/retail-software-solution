package me.ezra_home.retail_software_solution.platform.session


object SessionContextProvider {
    private val sessionContextThreadLocal = ThreadLocal<SessionContext?>()

    fun getSession(): SessionContext {
        return sessionContextThreadLocal.get() ?: SessionContext().also { sessionContextThreadLocal.set(it) }
    }

    fun clear() {
        sessionContextThreadLocal.remove()
    }
}
