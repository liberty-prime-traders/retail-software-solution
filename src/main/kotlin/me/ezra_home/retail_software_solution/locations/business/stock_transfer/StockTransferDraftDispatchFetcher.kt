package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferStatus
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component

@Component
class StockTransferDraftDispatchFetcher(
    private val stockTransferDispatchRepository: StockTransferDispatchRepository
) {

    fun requireDraftDispatch(orderReferenceNumber: String): StockTransferDispatchEntity {
        val dispatchEntity = stockTransferDispatchRepository.findByStockTransferOrderRef(orderReferenceNumber)
            ?: throw RtsGenericException("Dispatch not found for order $orderReferenceNumber")
        if (dispatchEntity.status != StockTransferStatus.DRAFT) {
            throw RtsGenericException("Operation only allowed in DRAFT status. Current: ${dispatchEntity.status}")
        }
        return dispatchEntity
    }
}
