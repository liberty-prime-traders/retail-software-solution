package me.ezra_home.retail_software_solution.organizations.business.tax_rate

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface TaxRateRepository : JpaRepository<TaxRateEntity, UUID> {

    @Query("""
        SELECT r FROM TaxRateEntity r
        WHERE r.startDate <= :date
        AND (r.endDate IS NULL OR r.endDate >= :date)
    """)
    fun findActiveRateByDate(date: LocalDate): List<TaxRateEntity>
}
