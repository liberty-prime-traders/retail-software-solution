package me.ezra_home.retail_software_solution.organizations.business.account

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Repository
interface AccountRepository : JpaRepository<AccountEntity, UUID> {
    fun findByCode(code: String): AccountEntity?

    fun existsByParentAccountCode(code: String): Boolean

    @Query("SELECT a.code as code, a.currentBalance as currentBalance FROM AccountEntity a WHERE a.code IN :codes")
    fun findBalancesByCodes(codes: Set<String>): List<AccountBalance>

    @Modifying
    @Query("UPDATE AccountEntity a SET a.currentBalance = a.currentBalance + :delta, a.balanceUpdatedAt = :now WHERE a.code = :code")
    fun incrementBalance(code: String, delta: BigDecimal, now: Instant)
}
