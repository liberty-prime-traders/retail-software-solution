package me.ezra_home.retail_software_solution.locations.business.purchase

import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseStatus
import java.math.BigDecimal

object PurchaseStatusResolver {

    fun resolveAfterQuantityChange(
        purchaseLines: List<PurchaseLineEntity>,
        statusWhenLinesOpenAndNothingDelivered: () -> PurchaseStatus = { PurchaseStatus.ORDERED }
    ): PurchaseStatus {
        val allAccountedFor = purchaseLines.none { it.storageQuantityRemaining().isPositive() }
        val anyDelivered = purchaseLines.any { it.storageQuantityDelivered > BigDecimal.ZERO }
        return when {
            allAccountedFor && !anyDelivered -> PurchaseStatus.CANCELED
            allAccountedFor -> PurchaseStatus.FULLY_DELIVERED
            anyDelivered -> PurchaseStatus.PARTIALLY_DELIVERED
            else -> statusWhenLinesOpenAndNothingDelivered()
        }
    }
}
