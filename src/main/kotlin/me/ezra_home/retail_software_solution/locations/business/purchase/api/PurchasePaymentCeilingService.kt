package me.ezra_home.retail_software_solution.locations.business.purchase.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryDataFetcher
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseLineEntity
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseLineRepository
import me.ezra_home.retail_software_solution.util.business.Decimals
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
@TransactionalOnLocationSchema(readOnly = true)
class PurchasePaymentCeilingService(
    private val purchaseLineRepository: PurchaseLineRepository,
    private val purchaseDeliveryDataFetcher: PurchaseDeliveryDataFetcher
) {

    data class PaymentCeiling(val amount: BigDecimal, val isFullyDelivered: Boolean, val deliveredTotal: BigDecimal)

    fun computeCeiling(purchaseId: UUID): PaymentCeiling {
        val lines = purchaseLineRepository.findByPurchaseId(purchaseId)
        val deliveredTotal = purchaseDeliveryDataFetcher.calculateDeliveredTotal(purchaseId)
        return computeCeiling(lines, deliveredTotal)
    }

    fun computeCeiling(lines: List<PurchaseLineEntity>, deliveredTotal: BigDecimal): PaymentCeiling {
        val poTotal = lines.sumOf { Decimals.multiplyScale4(it.unitCost, it.getExpectedQuantity()) }
        val isFullyDelivered = deliveredTotal > BigDecimal.ZERO &&
            lines.all { it.getRemainingQuantity() <= BigDecimal.ZERO }
        val ceiling = when {
            isFullyDelivered -> deliveredTotal
            deliveredTotal > BigDecimal.ZERO -> poTotal.max(deliveredTotal)
            else -> poTotal
        }
        return PaymentCeiling(amount = ceiling, isFullyDelivered = isFullyDelivered, deliveredTotal = deliveredTotal)
    }
}
