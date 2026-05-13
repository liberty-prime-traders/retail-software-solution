package me.ezra_home.retail_software_solution.locations.business.lock

import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EntityAdvisoryLock(
    private val entityAdvisoryLockRepository: EntityAdvisoryLockRepository,
) {
    // Sorted acquisition prevents two-threads-different-order deadlocks. Namespace prevents
    // hash collisions across entity types — a sale UUID can never share a lock key with a product UUID.
    fun acquire(namespace: String, ids: Collection<UUID>) {
        ids.toSortedSet().forEach { entityAdvisoryLockRepository.acquire("$namespace:$it") }
    }

    fun acquire(namespace: String, id: UUID) {
        acquire(namespace, listOf(id))
    }
}
