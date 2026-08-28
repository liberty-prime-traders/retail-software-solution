package me.ezra_home.retail_software_solution.organizations.business.lock

import me.ezra_home.retail_software_solution.util.business.lock.AdvisoryLock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OrgEntityAdvisoryLockRepository : JpaRepository<LockRoutingEntity, UUID>, AdvisoryLock.Repository {

    @Query(
        value = "SELECT pg_advisory_xact_lock(hashtextextended(:key, 0))",
        nativeQuery = true
    )
    override fun acquire(key: String)
}
