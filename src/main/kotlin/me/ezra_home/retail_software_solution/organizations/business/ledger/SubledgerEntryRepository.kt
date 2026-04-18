package me.ezra_home.retail_software_solution.organizations.business.ledger

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SubledgerEntryRepository : JpaRepository<SubledgerEntryEntity, UUID> {

    @Query("""
        SELECT s.* FROM subledger_entry s
        WHERE s.id IN (
            SELECT DISTINCT ON (contact_reference_number) id
            FROM subledger_entry
            WHERE contact_reference_number IN :contactReferenceNumbers
            ORDER BY contact_reference_number, created_on DESC
        )
        FOR UPDATE
    """, nativeQuery = true)
    fun findLatestForContacts(contactReferenceNumbers: Set<String>): List<SubledgerEntryEntity>
}
