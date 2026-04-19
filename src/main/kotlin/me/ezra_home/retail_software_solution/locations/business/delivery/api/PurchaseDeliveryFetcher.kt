package me.ezra_home.retail_software_solution.locations.business.delivery.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryAssembler
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryRepository
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineDto
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema(readOnly = true)
class PurchaseDeliveryFetcher(
    private val purchaseDeliveryRepository: PurchaseDeliveryRepository,
    private val purchaseDeliveryAssembler: PurchaseDeliveryAssembler
) {

    fun getDeliveryReferenceNumbersById(deliveryIds: List<UUID>): Map<UUID, String> {
        return purchaseDeliveryRepository.findAllById(deliveryIds)
            .mapNotNull { entity -> entity.referenceNumber?.let { entity.id!! to it } }
            .toMap()
    }

    fun getDeliveryResponses(
        purchaseIds: List<UUID>,
        purchaseLineById: Map<UUID, PurchaseLineDto>,
        productSummaries: Map<UUID, LocationProductSummaryDto>
    ): Map<UUID, List<PurchaseDeliveryResponseDto>> {
        val allDeliveries = purchaseDeliveryRepository.findByPurchaseIdIn(purchaseIds)
        val deliveriesByPurchaseId = allDeliveries.groupBy { it.purchaseId }
        return deliveriesByPurchaseId.mapValues { (_, deliveries) ->
            purchaseDeliveryAssembler.buildResponses(deliveries, purchaseLineById, productSummaries)
        }
    }
}
