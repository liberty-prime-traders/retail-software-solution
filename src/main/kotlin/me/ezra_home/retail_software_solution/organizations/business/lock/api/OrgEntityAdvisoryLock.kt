package me.ezra_home.retail_software_solution.organizations.business.lock.api

import me.ezra_home.retail_software_solution.organizations.business.lock.OrgEntityAdvisoryLockRepository
import me.ezra_home.retail_software_solution.util.business.lock.AdvisoryLock
import org.springframework.stereotype.Component

@Component
class OrgEntityAdvisoryLock(
    private val orgEntityAdvisoryLockRepository: OrgEntityAdvisoryLockRepository,
) {
    fun acquire(namespace: String, keys: Collection<String>) {
        AdvisoryLock.acquire(orgEntityAdvisoryLockRepository, namespace, keys)
    }

    fun acquire(namespace: String, key: String) {
        acquire(namespace, listOf(key))
    }
}
