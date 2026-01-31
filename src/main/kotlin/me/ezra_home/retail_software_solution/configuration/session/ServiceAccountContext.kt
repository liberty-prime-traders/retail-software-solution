package me.ezra_home.retail_software_solution.configuration.session

import me.ezra_home.retail_software_solution.util.enums.ServiceAccount

object ServiceAccountContext {

    fun <T> runWithServiceAccount(serviceAccount: ServiceAccount, block: () -> T): T {
        val sessionContext = SessionContextProvider.getSession()
        val previousUserId = sessionContext.systemUserId
        sessionContext.systemUserId = serviceAccount.uniqueId

        return try {
            block()
        } finally {
            sessionContext.systemUserId = previousUserId
        }
    }
}
