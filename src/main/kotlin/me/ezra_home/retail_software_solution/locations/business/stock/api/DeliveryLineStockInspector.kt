package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema(readOnly = true)
class DeliveryLineStockInspector(
    private val stockMovementRepository: StockMovementRepository
) {

    fun hasTransferredOut(deliveryReferenceNumber: String, deliveryLineReferenceNumber: String): Boolean =
        stockMovementRepository.countNetTransferredOutOnEntriesOfSource(
            deliveryReferenceNumber,
            deliveryLineReferenceNumber,
            MovementType.TRANSFER_OUT,
            MovementType.TRANSFER_CANCELLED
        ) > 0

    fun hasMovementsThatCannotBeReversed(deliveryReferenceNumber: String, deliveryLineReferenceNumber: String): Boolean =
        stockMovementRepository.countMovementsOfTypesOnEntriesOfSource(
            deliveryReferenceNumber,
            deliveryLineReferenceNumber,
            MovementType.movementsThatCannotBeReversed
        ) > 0
}
