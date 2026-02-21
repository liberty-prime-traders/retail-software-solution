package me.ezra_home.retail_software_solution.configuration.session

import me.ezra_home.retail_software_solution.util.enums.ServiceAccount

object ServiceAccountContext {

    fun <T> runWithServiceAccount(serviceAccount: ServiceAccount, block: () -> T): T {
        val sessionSnapshot = SessionContextProvider.getSession().copy()
        SessionContextProvider.initSystemUser(serviceAccount.uniqueId)
        return try {
            block()
        } finally {
            SessionContextProvider.setSession(sessionSnapshot)
        }
    }

    fun runWithServiceAccount(serviceAccount: ServiceAccount, block: () -> Unit) {
        runWithServiceAccount<Unit>(serviceAccount) { block() }
    }
}
