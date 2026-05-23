package me.ezra_home.retail_software_solution.locations.business.sale

import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SaleStockReserver(
    private val reservationRepository: SaleLineStockReservationRepository,
) {

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
}
