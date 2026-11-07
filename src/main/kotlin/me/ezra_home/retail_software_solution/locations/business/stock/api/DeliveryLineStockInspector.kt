package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema(readOnly = true)
class DeliveryLineStockInspector(
    private val stockMovementRepository: StockMovementRepository,
    private val stockEntryRepository: StockEntryRepository
) {

    fun getStockEntryId(deliveryReferenceNumber: String, deliveryLineReferenceNumber: String): UUID =
        stockEntryRepository.findIdsByDocumentAndDetailReference(deliveryReferenceNumber, deliveryLineReferenceNumber).singleOrNull()
            ?: throw RtsGenericException("Delivery line $deliveryLineReferenceNumber has no single stock layer.")

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
