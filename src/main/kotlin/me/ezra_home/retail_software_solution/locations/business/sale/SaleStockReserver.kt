package me.ezra_home.retail_software_solution.locations.business.sale

import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
class SaleStockReserver(
    private val reservationRepository: SaleLineStockReservationRepository,
) {

    fun loadReservedTotals(locationProductIds: Collection<UUID>): Map<UUID, BigDecimal> {
        if (locationProductIds.isEmpty()) return emptyMap()
        val breakdown = loadReservationBreakdown(locationProductIds)
        return locationProductIds.associateWith { breakdown[it]?.total ?: BigDecimal.ZERO }
    }

    fun loadReservationBreakdown(productIds: Collection<UUID>): Map<UUID, ProductReservations> {
        if (productIds.isEmpty()) return emptyMap()
        return reservationRepository.findProductSaleReservations(productIds)
            .groupBy { it.locationProductId }
            .mapValues { (_, rows) ->
                ProductReservations(
                    total = rows.sumOf { it.quantityReserved },
                    bySale = rows.associate { it.saleId to it.quantityReserved }
                )
            }
    }

    fun reserve(saleId: UUID, lines: List<SaleLineEntity>) {
        if (lines.isEmpty()) return
        val reservations = lines.map { line ->
            SaleLineStockReservationEntity(
                saleId = saleId,
                saleLineId = line.id!!,
                locationProductId = line.locationProductId,
                quantityReserved = line.baseQty(),
            )
        }
        reservationRepository.saveAll(reservations)
    }

    fun clearBySale(saleId: UUID) {
        reservationRepository.deleteBySaleId(saleId)
    }

    fun syncUpdatedReservations(
        updatedLines: List<SaleLineEntity>,
        newLines: List<SaleLineEntity>,
        saleId: UUID,
    ) {
        val updatedLinesWithQuantityChanges = if (updatedLines.isEmpty()) emptyList() else {
            val existingBySaleLineId = reservationRepository
                .findBySaleLineIdIn(updatedLines.map { it.id!! })
                .associateBy { it.saleLineId }
            updatedLines.filter {
                existingBySaleLineId[it.id!!]?.quantityReserved?.compareTo(it.baseQty()) != 0
            }
        }
        if (updatedLinesWithQuantityChanges.isNotEmpty()) {
            reservationRepository.deleteBySaleLineIdIn(updatedLinesWithQuantityChanges.map { it.id!! })
        }
        reserve(saleId, updatedLinesWithQuantityChanges + newLines)
    }
}
