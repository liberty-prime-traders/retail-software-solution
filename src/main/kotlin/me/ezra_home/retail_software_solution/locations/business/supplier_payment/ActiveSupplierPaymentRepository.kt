package me.ezra_home.retail_software_solution.locations.business.supplier_payment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ActiveSupplierPaymentRepository : JpaRepository<ActiveSupplierPaymentEntity, UUID> {

    fun findByPurchaseId(purchaseId: UUID): List<ActiveSupplierPaymentEntity>

    fun findByDeliveryId(deliveryId: UUID): List<ActiveSupplierPaymentEntity>
}
