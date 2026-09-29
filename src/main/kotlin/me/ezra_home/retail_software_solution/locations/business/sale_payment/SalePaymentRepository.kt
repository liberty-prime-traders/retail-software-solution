package me.ezra_home.retail_software_solution.locations.business.sale_payment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SalePaymentRepository : JpaRepository<SalePaymentEntity, UUID> {
    fun findBySaleId(saleId: UUID): List<SalePaymentEntity>
    fun findBySaleIdIn(saleIds: List<UUID>): List<SalePaymentEntity>
}
