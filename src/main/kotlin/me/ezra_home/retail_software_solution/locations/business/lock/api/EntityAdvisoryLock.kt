package me.ezra_home.retail_software_solution.locations.business.lock.api

import me.ezra_home.retail_software_solution.locations.business.lock.EntityAdvisoryLockRepository
import me.ezra_home.retail_software_solution.util.business.lock.AdvisoryLock
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EntityAdvisoryLock(
    private val entityAdvisoryLockRepository: EntityAdvisoryLockRepository,
) {
    fun acquire(namespace: String, ids: Collection<UUID>) {
        AdvisoryLock.acquire(entityAdvisoryLockRepository, namespace, ids.map(UUID::toString))
    }

    fun acquire(namespace: String, id: UUID) {
        acquire(namespace, listOf(id))
    }
}
