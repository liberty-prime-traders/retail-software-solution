package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
class StockAvailabilityValidator(
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val stockReserver: StockReserver,
    private val stockBreakdownProvider: StockBreakdownProvider,
    private val entityAdvisoryLock: EntityAdvisoryLock,
) {

    fun guardSufficientStock(
        storageQuantityNeededByLocationProductId: Map<UUID, ExactQuantity>,
        productLabelByLocationProductId: Map<UUID, String> = emptyMap(),
    ) {
        if (storageQuantityNeededByLocationProductId.isEmpty()) return
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, storageQuantityNeededByLocationProductId.keys)
        val balances = stockBalanceFetcher.getLatestBalances(storageQuantityNeededByLocationProductId.keys)
        val reservations = stockReserver.loadReservationBreakdown(storageQuantityNeededByLocationProductId.keys)
        val breakdownByLocationProductId = stockBreakdownProvider.forLocationProducts(storageQuantityNeededByLocationProductId.keys)
        storageQuantityNeededByLocationProductId.forEach { (locationProductId, storageQuantityNeeded) ->
            val reservedTotal = reservations[locationProductId]?.total ?: BigDecimal.ZERO
            val available = ExactQuantity.ofStorage(balances.getValue(locationProductId)) - ExactQuantity.ofStorage(reservedTotal)
            if (available < storageQuantityNeeded) {
                val label = productLabelByLocationProductId[locationProductId] ?: locationProductId.toString()
                val breakdown = breakdownByLocationProductId.getValue(locationProductId)
                throw RtsGenericException(
                    "Insufficient stock for $label. Available: ${breakdown.render(available)}, Requested: ${breakdown.render(storageQuantityNeeded)}"
                )
            }
        }
    }
}
