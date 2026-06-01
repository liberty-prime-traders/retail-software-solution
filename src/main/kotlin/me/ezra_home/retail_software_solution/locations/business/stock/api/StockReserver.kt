package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.locations.business.stock.StockReservationEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockReservationRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class StockReserver(
    private val stockReservationRepository: StockReservationRepository,
) {

    fun loadReservationBreakdown(locationProductIds: Collection<UUID>): Map<UUID, ProductReservations> {
        if (locationProductIds.isEmpty()) return emptyMap()
        return stockReservationRepository.findProductReservations(locationProductIds)
            .groupBy { it.locationProductId }
            .mapValues { (_, rows) ->
                ProductReservations(
                    total = rows.sumOf { it.quantityReserved },
                    bySale = rows.associate { it.saleId to it.quantityReserved },
                )
            }
    }

    fun reserve(newStockReservations: List<NewStockReservation>) {
        if (newStockReservations.isEmpty()) return
        val entities = newStockReservations.map {
            StockReservationEntity(
                saleId = it.saleId,
                saleLineId = it.saleLineId,
                locationProductId = it.locationProductId,
                quantityReserved = it.quantityReserved,
            )
        }
        stockReservationRepository.saveAll(entities)
    }

    fun clearBySale(saleId: UUID) {
        stockReservationRepository.deleteBySaleId(saleId)
    }

    fun clearBySaleLineIds(saleLineIds: List<UUID>) {
        if (saleLineIds.isEmpty()) return
        stockReservationRepository.deleteBySaleLineIdIn(saleLineIds)
    }
}
