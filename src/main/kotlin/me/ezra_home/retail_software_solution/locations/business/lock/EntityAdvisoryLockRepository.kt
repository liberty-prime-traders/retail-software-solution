package me.ezra_home.retail_software_solution.locations.business.lock

import me.ezra_home.retail_software_solution.locations.business.sale.SaleEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

// The entity binding is incidental; this repository exists only to route a namespace-agnostic
// advisory-lock query through the location-schema EntityManagerFactory.
@Repository
interface EntityAdvisoryLockRepository : JpaRepository<SaleEntity, UUID> {

    @Query(
        value = "SELECT pg_advisory_xact_lock(hashtextextended(:key, 0))",
        nativeQuery = true
    )
    fun acquire(key: String)
}
