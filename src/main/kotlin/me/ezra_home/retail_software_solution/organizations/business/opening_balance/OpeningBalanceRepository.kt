package me.ezra_home.retail_software_solution.organizations.business.opening_balance

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface OpeningBalanceRepository : JpaRepository<OpeningBalanceEntity, UUID> {

    fun findByAccountCodeOrderByCreatedOnAsc(accountCode: String): List<OpeningBalanceEntity>

    fun findFirstByAccountCodeAndCreatedOnLessThanOrderByCreatedOnDesc(
        accountCode: String,
        createdOn: OffsetDateTime
    ): OpeningBalanceEntity?

    @Query("""
        SELECT * FROM opening_balance
        WHERE account_code = :accountCode
        ORDER BY created_on DESC
        LIMIT 1
    """, nativeQuery = true)
    fun findLatestForAccountCode(accountCode: String): OpeningBalanceEntity?

    @Query("""
        SELECT * FROM opening_balance
        WHERE id IN (
            SELECT DISTINCT ON (account_code) id
            FROM opening_balance
            WHERE account_code IN :accountCodes
            ORDER BY account_code, created_on DESC
        )
    """, nativeQuery = true)
    fun findLatestForAccountCodes(accountCodes: Set<String>): List<OpeningBalanceEntity>
}
