package me.ezra_home.retail_software_solution.locations.business.delivery.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryLineRepository
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LastPurchaseCostUpdateDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.delivery.PurchaseDeliveryLineStatus
import me.ezra_home.retail_software_solution.locations.business.purchase.api.delivery.PurchaseDeliveryStatus
import org.springframework.stereotype.Service
import java.util.UUID

// Short-lived: deleted with the rest of the last purchase cost backfill once every organization has run it.
@Service
@TransactionalOnLocationSchema(readOnly = true)
class LastPurchaseCostBackfillStore(
    private val purchaseDeliveryLineRepository: PurchaseDeliveryLineRepository,
) {

    fun findLastDeliveredCostsAfter(
        afterLocationProductId: UUID,
        batchSize: Int
    ): List<LastPurchaseCostUpdateDto> =
        purchaseDeliveryLineRepository.findLastDeliveredCostsAfter(
            PurchaseDeliveryStatus.RECEIVED.code,
            PurchaseDeliveryLineStatus.ACTIVE.code,
            afterLocationProductId,
            batchSize
        ).map {
            LastPurchaseCostUpdateDto(
                locationProductId = it.locationProductId,
                unitCost = it.unitCost,
                storageUnitsPerUserProvidedUnit = it.storageUnitsPerUserProvidedUnit,
            )
        }
}
