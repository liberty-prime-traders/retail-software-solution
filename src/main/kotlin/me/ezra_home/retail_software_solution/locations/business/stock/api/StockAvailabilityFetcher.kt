package me.ezra_home.retail_software_solution.locations.business.stock.api

import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

data class StockAvailability(
    val quantityOnHand: BigDecimal,
    val quantityReserved: BigDecimal,
    val quantityAvailable: BigDecimal,
) {
    companion object {
        val ZERO = StockAvailability(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
    }
}

@Component
class StockAvailabilityFetcher(
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val stockReserver: StockReserver,
) {

    fun fetch(locationProductIds: Collection<UUID>): Map<UUID, StockAvailability> {
        if (locationProductIds.isEmpty()) return emptyMap()
        val balancesByLocationProductId = stockBalanceFetcher.getLatestBalances(locationProductIds)
        val reservationsByLocationProductId = stockReserver.loadReservationBreakdown(locationProductIds)
        return locationProductIds.associateWith { locationProductId ->
            val quantityOnHand = balancesByLocationProductId[locationProductId] ?: BigDecimal.ZERO
            val quantityReserved = reservationsByLocationProductId[locationProductId]?.total ?: BigDecimal.ZERO
            StockAvailability(
                quantityOnHand = quantityOnHand,
                quantityReserved = quantityReserved,
                quantityAvailable = quantityOnHand - quantityReserved,
            )
        }
    }
}
