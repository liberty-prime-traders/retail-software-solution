package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDataFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.read.StockBalanceFetcher
import me.ezra_home.retail_software_solution.locations.business.stock_reservation.api.StockReserver
import me.ezra_home.retail_software_solution.util.exceptions.BlockingProblem
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class ReservationShortfallChecker(
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val stockReserver: StockReserver,
    private val saleDataFetcher: SaleDataFetcher,
    private val locationProductDataFetcher: LocationProductDataFetcher,
) {

    fun problemsAfterPendingChange(
        locationProductId: UUID,
        pendingStorageQuantityChange: ExactQuantity,
        quantityBreakdown: QuantityBreakdown
    ): List<BlockingProblem> {
        val productReservations = stockReserver.loadReservationBreakdown(listOf(locationProductId))[locationProductId]
            ?: return emptyList()
        val onHandAfterChange = ExactQuantity.ofStorage(
            stockBalanceFetcher.getLatestBalances(listOf(locationProductId)).getValue(locationProductId)
        ) + pendingStorageQuantityChange
        val reservedTotal = ExactQuantity.ofStorage(productReservations.total)
        if (reservedTotal <= onHandAfterChange) return emptyList()

        val productName = locationProductDataFetcher.findSummaryByIds(listOf(locationProductId))
            .getValue(locationProductId).productName
        val referenceNumberBySaleId = saleDataFetcher.getReferenceNumbersById(productReservations.bySale.keys)
        val eventualOnHandBreakdown = quantityBreakdown.render(onHandAfterChange)
        val reservedBreakdown = quantityBreakdown.render(reservedTotal)
        val holdersText = if (productReservations.bySale.size == 1) {
            "the draft sale holds $reservedBreakdown"
        } else {
            "the draft sales hold $reservedBreakdown between them"
        }
        val effect = "After your change only $eventualOnHandBreakdown would be left in stock, but $holdersText."
        return productReservations.bySale
            .map { (saleId, reservedQuantity) ->
                BlockingProblem(
                    documentReference = "${referenceNumberBySaleId.getValue(saleId)} (draft)",
                    description = "Draft sale holding " +
                        "${quantityBreakdown.render(ExactQuantity.ofStorage(reservedQuantity))} of $productName.",
                    effect = effect
                )
            }
            .sortedBy { it.documentReference }
    }
}
