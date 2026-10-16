package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAvailability
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAvailabilityFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockReserver
import me.ezra_home.retail_software_solution.util.business.Decimals
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class SaleSessionStockOverlay(
    private val stockAvailabilityFetcher: StockAvailabilityFetcher,
    private val stockReserver: StockReserver,
) {

    fun populate(saleSession: SaleSession): SaleSession {
        if (saleSession.originalStatus != SaleStatus.DRAFT) return saleSession
        if (saleSession.saleLines.isEmpty()) return saleSession
        val locationProductIds = saleSession.saleLines.map { it.locationProductId }.distinct()
        val availabilityByLocationProductId = stockAvailabilityFetcher.fetch(locationProductIds)
        val reservationsByLocationProductId = stockReserver.loadReservationBreakdown(locationProductIds)
        val populatedSaleLines = saleSession.saleLines.map { saleSessionLine ->
            val availability = availabilityByLocationProductId[saleSessionLine.locationProductId] ?: StockAvailability.ZERO
            val ownReservation = saleSession.saleId?.let { saleId ->
                reservationsByLocationProductId[saleSessionLine.locationProductId]?.forSale(saleId)
            } ?: BigDecimal.ZERO
            saleSessionLine.copy(
                quantityOnHand = Decimals.Scale4.round(availability.quantityOnHand),
                quantityReserved = Decimals.Scale4.round(availability.quantityReserved.subtract(ownReservation)),
                quantityAvailable = Decimals.Scale4.round(availability.quantityAvailable.add(ownReservation)),
            )
        }
        return saleSession.copy(saleLines = populatedSaleLines)
    }
}
