package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
class StockAvailabilityValidator(
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val stockReserver: StockReserver,
    private val entityAdvisoryLock: EntityAdvisoryLock,
) {

    fun guardSufficientStock(
        quantityNeededByLocationProductId: Map<UUID, BigDecimal>,
        productLabelByLocationProductId: Map<UUID, String> = emptyMap(),
    ) {
        if (quantityNeededByLocationProductId.isEmpty()) return
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, quantityNeededByLocationProductId.keys)
        val balances = stockBalanceFetcher.getLatestBalances(quantityNeededByLocationProductId.keys)
        val reservations = stockReserver.loadReservationBreakdown(quantityNeededByLocationProductId.keys)
        quantityNeededByLocationProductId.forEach { (locationProductId, quantityNeeded) ->
            val reservedTotal = reservations[locationProductId]?.total ?: BigDecimal.ZERO
            val available = balances.getValue(locationProductId).subtract(reservedTotal)
            if (available < quantityNeeded) {
                val label = productLabelByLocationProductId[locationProductId] ?: locationProductId.toString()
                val formattedAvailable = Decimals.stripZeroesAndRound(available)
                val formattedRequested = Decimals.stripZeroesAndRound(quantityNeeded)
                throw RtsGenericException("Insufficient stock for $label. Available: $formattedAvailable, Requested: $formattedRequested")
            }
        }
    }
}
