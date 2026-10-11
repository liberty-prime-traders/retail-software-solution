package me.ezra_home.retail_software_solution.organizations.business.ledger

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface SubledgerEntryRepository : JpaRepository<SubledgerEntryEntity, UUID> {

    @Query("""
        SELECT s.contactReferenceNumber AS contactReferenceNumber,
               SUM(s.payableAmount) AS payableTotal,
               SUM(s.receivableAmount) AS receivableTotal
        FROM SubledgerEntryEntity s
        WHERE s.contactReferenceNumber IN :contactReferenceNumbers
        GROUP BY s.contactReferenceNumber
    """)
    fun sumAmountsForContacts(contactReferenceNumbers: Set<String>): List<SubledgerAmountTotals>

    @Query("""
        SELECT s.* FROM subledger_entry s
        WHERE s.contact_reference_number = :contactReferenceNumber
        ORDER BY s.created_on DESC, s.id DESC
        LIMIT :limit
    """, nativeQuery = true)
    fun findFirstPage(contactReferenceNumber: String, limit: Int): List<SubledgerEntryEntity>

    @Query("""
        SELECT s.* FROM subledger_entry s
        WHERE s.contact_reference_number = :contactReferenceNumber
          AND (s.created_on < :cursorCreatedOn OR (s.created_on = :cursorCreatedOn AND s.id < :cursorId))
        ORDER BY s.created_on DESC, s.id DESC
        LIMIT :limit
    """, nativeQuery = true)
    fun findPageAfter(
        contactReferenceNumber: String,
        cursorCreatedOn: OffsetDateTime,
        cursorId: UUID,
        limit: Int
    ): List<SubledgerEntryEntity>
}
