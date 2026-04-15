package me.ezra_home.retail_software_solution.organizations.business.fiscal_period

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface FiscalPeriodRepository : JpaRepository<FiscalPeriodEntity, UUID> {

    fun findTopByOrderByEndDateDesc(): FiscalPeriodEntity?

    fun findByIdIn(ids: Set<UUID>): List<FiscalPeriodEntity>

    @Query("SELECT p FROM FiscalPeriodEntity p WHERE p.startDate >= :from AND p.startDate <= :to")
    fun findPeriodsInGivenYear(from: LocalDate, to: LocalDate): Collection<FiscalPeriodEntity>

    @Query("SELECT COUNT(p) FROM FiscalPeriodEntity p WHERE p.startDate >= :from AND p.startDate <= :to AND p.closedAt IS NULL AND p.id <> :excludeId")
    fun countOpenInYear(from: LocalDate, to: LocalDate, excludeId: UUID): Int
}
