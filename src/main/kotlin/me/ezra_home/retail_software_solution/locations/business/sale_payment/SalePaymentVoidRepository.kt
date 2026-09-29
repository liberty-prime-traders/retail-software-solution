package me.ezra_home.retail_software_solution.locations.business.sale_payment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SalePaymentVoidRepository : JpaRepository<SalePaymentVoidEntity, UUID> {
    fun findBySalePaymentIdIn(salePaymentIds: List<UUID>): List<SalePaymentVoidEntity>
    fun existsBySalePaymentId(salePaymentId: UUID): Boolean
}
