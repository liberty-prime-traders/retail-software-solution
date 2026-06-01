package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.stock.api.ProductReservations
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBalanceFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockReserver
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class SaleSessionStockOverlay(
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val stockReserver: StockReserver,
) {

    fun populate(saleSession: SaleSession): SaleSession {
        if (saleSession.originalStatus != SaleStatus.DRAFT) return saleSession
        if (saleSession.saleLines.isEmpty()) return saleSession
        val locationProductIds = saleSession.saleLines.map { it.locationProductId }.distinct()
        val quantityOnHandByLocationProductId = stockBalanceFetcher.getLatestBalances(locationProductIds)
        val reservationsByLocationProductId = stockReserver.loadReservationBreakdown(locationProductIds)
        val populatedSaleLines = saleSession.saleLines.map { saleSessionLine ->
            val quantityOnHand = quantityOnHandByLocationProductId[saleSessionLine.locationProductId] ?: BigDecimal.ZERO
            val quantityReservedByOthers = reservedByOthers(
                productReservations = reservationsByLocationProductId[saleSessionLine.locationProductId],
                excludingSaleId = saleSession.saleId,
            )
            saleSessionLine.copy(
                quantityOnHand = quantityOnHand,
                quantityReserved = quantityReservedByOthers,
                quantityAvailable = quantityOnHand.subtract(quantityReservedByOthers),
            )
        }
        return saleSession.copy(saleLines = populatedSaleLines)
    }

    private fun reservedByOthers(productReservations: ProductReservations?, excludingSaleId: UUID?): BigDecimal {
        if (productReservations == null) return BigDecimal.ZERO
        return excludingSaleId?.let { productReservations.excludingSale(it) } ?: productReservations.total
    }
}
