package me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance

import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactSubledgerDirection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface ContactOpeningBalanceRepository : JpaRepository<ContactOpeningBalanceEntity, UUID> {

    fun findFirstByContactReferenceNumberAndSubledgerDirectionOrderByCreatedOnDesc(
        contactReferenceNumber: String,
        subledgerDirection: ContactSubledgerDirection
    ): ContactOpeningBalanceEntity?

    fun findFirstByContactReferenceNumberAndSubledgerDirectionAndCreatedOnLessThanOrderByCreatedOnDesc(
        contactReferenceNumber: String,
        subledgerDirection: ContactSubledgerDirection,
        createdOn: OffsetDateTime
    ): ContactOpeningBalanceEntity?

    fun findByContactReferenceNumberOrderByCreatedOnAsc(contactReferenceNumber: String): List<ContactOpeningBalanceEntity>

    @Query("""
        SELECT * FROM contact_opening_balance
        WHERE id IN (
            SELECT DISTINCT ON (contact_reference_number, subledger_direction) id
            FROM contact_opening_balance
            WHERE contact_reference_number IN :contactReferenceNumbers
            ORDER BY contact_reference_number, subledger_direction, created_on DESC
        )
    """, nativeQuery = true)
    fun findLatestForContacts(contactReferenceNumbers: Set<String>): List<ContactOpeningBalanceEntity>
}
