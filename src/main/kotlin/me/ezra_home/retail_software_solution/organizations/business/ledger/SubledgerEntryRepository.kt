package me.ezra_home.retail_software_solution.organizations.business.ledger

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SubledgerEntryRepository : JpaRepository<SubledgerEntryEntity, UUID> {

    @Query("""
        SELECT s.* FROM subledger_entry s
        INNER JOIN (
            SELECT contact_reference_number, MAX(created_on) AS max_created_on
            FROM subledger_entry
            WHERE contact_reference_number IN :contactReferenceNumbers
            GROUP BY contact_reference_number
        ) latest 
        ON s.contact_reference_number = latest.contact_reference_number
               AND s.created_on = latest.max_created_on
        FOR UPDATE 
    """, nativeQuery = true)
    fun findLatestForContacts(contactReferenceNumbers: Set<String>): List<SubledgerEntryEntity>
}
