package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderDataFetcher
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderDomainDto
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import org.springframework.stereotype.Component

@Component
class StockTransferOrderLocker(
    private val stockTransferOrderDataFetcher: StockTransferOrderDataFetcher,
    private val entityAdvisoryLock: EntityAdvisoryLock
) {

    // A status read before the lock can be stale behind a concurrent dispatch, cancel or completion, so the caller
    // decides on the order this returns. Needs an enclosing location transaction, as the lock does.
    fun lockThenFetch(orderReferenceNumber: String): StockTransferOrderDomainDto {
        val orderId = stockTransferOrderDataFetcher.getByReferenceNumber(orderReferenceNumber).id
        entityAdvisoryLock.acquire(LockNamespaces.STOCK_TRANSFER_ORDER, orderId)
        return stockTransferOrderDataFetcher.getByReferenceNumber(orderReferenceNumber)
    }
}
