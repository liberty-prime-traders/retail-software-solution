package me.ezra_home.retail_software_solution.organizations.business.ledger

import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SubledgerEntryRepository : JpaRepository<SubledgerEntryEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SubledgerEntryEntity s WHERE s.contactReferenceNumber = :ref ORDER BY s.createdOn DESC")
    fun findLatestForContact(@Param("ref") contactReferenceNumber: String, pageable: Pageable): List<SubledgerEntryEntity>
}
