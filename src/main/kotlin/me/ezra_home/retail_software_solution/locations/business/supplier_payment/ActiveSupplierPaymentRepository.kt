package me.ezra_home.retail_software_solution.locations.business.supplier_payment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.UUID

interface PaidAmountByPurchase {
    val purchaseId: UUID
    val paidAmount: BigDecimal
}

@Repository
interface ActiveSupplierPaymentRepository : JpaRepository<ActiveSupplierPaymentEntity, UUID> {

    @Query(
        "select payment.purchaseId as purchaseId, sum(payment.amount) as paidAmount " +
            "from ActiveSupplierPaymentEntity payment where payment.purchaseId in :purchaseIds " +
            "group by payment.purchaseId"
    )
    fun sumAmountsByPurchaseIdIn(purchaseIds: Collection<UUID>): List<PaidAmountByPurchase>

    @Query(
        "select coalesce(sum(payment.amount), 0) from ActiveSupplierPaymentEntity payment " +
            "where payment.deliveryId = :deliveryId"
    )
    fun sumAmountByDeliveryId(deliveryId: UUID): BigDecimal
}
