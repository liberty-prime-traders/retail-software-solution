package me.ezra_home.retail_software_solution.organizations.business.ledger

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface LedgerEntryRepository : JpaRepository<LedgerEntryEntity, UUID> {

    @Query("""
        SELECT width_bucket(g.posted_on, CAST(:dayStartsLiteral AS timestamptz[])) AS "bucket",
               e.account_code AS "accountCode",
               e.entry_type AS "entryTypeCode",
               g.source_type AS "sourceTypeCode",
               SUM(e.amount) AS "amount"
        FROM ledger_entry e
        JOIN ledger_entry_group g ON g.reference_number = e.group_reference_number
        WHERE g.posted_on >= :rangeStart AND g.posted_on < :rangeEnd
          AND e.account_code IN (:accountCodes)
          AND g.source_type IN (:sourceTypeCodes)
        GROUP BY 1, 2, 3, 4
    """, nativeQuery = true)
    fun sumMovementsInRange(
        dayStartsLiteral: String, // e.g. '{2024-01-01T00:00:00Z, 2024-01-02T00:00:00Z, 2024-01-03T00:00:00Z}'
        rangeStart: Instant,
        rangeEnd: Instant,
        accountCodes: Set<String>,
        sourceTypeCodes: Set<String>
    ): List<LedgerBucketMovement>

    @Query("""
        SELECT width_bucket(g.posted_on, CAST(:dayStartsLiteral AS timestamptz[])) AS "bucket",
               e.account_code AS "accountCode",
               e.entry_type AS "entryTypeCode",
               g.source_type AS "sourceTypeCode",
               SUM(e.amount) AS "amount"
        FROM ledger_entry e
        JOIN ledger_entry_group g ON g.reference_number = e.group_reference_number
        WHERE g.posted_on >= :rangeStart AND g.posted_on < :rangeEnd
          AND e.account_code IN (:accountCodes)
          AND g.source_type IN (:sourceTypeCodes)
          AND g.source_location_id = :locationId
        GROUP BY 1, 2, 3, 4
    """, nativeQuery = true)
    fun sumMovementsInRangeForLocation(
        dayStartsLiteral: String,
        rangeStart: Instant,
        rangeEnd: Instant,
        accountCodes: Set<String>,
        sourceTypeCodes: Set<String>,
        locationId: UUID
    ): List<LedgerBucketMovement>

    @Query("""
        SELECT width_bucket(g.posted_on, CAST(:dayStartsLiteral AS timestamptz[])) AS "bucket",
               e.account_code AS "accountCode",
               e.entry_type AS "entryTypeCode",
               SUM(e.amount) AS "amount"
        FROM ledger_entry e
        JOIN ledger_entry_group g ON g.reference_number = e.group_reference_number
        WHERE g.posted_on >= :rangeStart
          AND e.account_code IN (:accountCodes)
        GROUP BY 1, 2, 3
    """, nativeQuery = true)
    fun sumAccountMovementsFrom(
        dayStartsLiteral: String,
        rangeStart: Instant,
        accountCodes: Set<String>
    ): List<LedgerAccountBucketMovement>
}
