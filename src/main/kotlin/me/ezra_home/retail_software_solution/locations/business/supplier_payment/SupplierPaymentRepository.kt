package me.ezra_home.retail_software_solution.locations.business.supplier_payment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SupplierPaymentRepository : JpaRepository<SupplierPaymentEntity, UUID> {
    fun findByPurchaseId(purchaseId: UUID): List<SupplierPaymentEntity>
    fun findByDeliveryId(deliveryId: UUID): List<SupplierPaymentEntity>
}
