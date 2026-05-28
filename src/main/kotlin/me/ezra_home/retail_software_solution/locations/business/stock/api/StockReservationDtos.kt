package me.ezra_home.retail_software_solution.locations.business.stock.api

import java.math.BigDecimal
import java.util.UUID

data class ProductReservationRow(
    val locationProductId: UUID,
    val saleId: UUID,
    val quantityReserved: BigDecimal,
)

data class ProductReservations(
    val total: BigDecimal,
    val bySale: Map<UUID, BigDecimal>,
) {
    fun forSale(saleId: UUID): BigDecimal = bySale[saleId] ?: BigDecimal.ZERO
    fun excludingSale(saleId: UUID): BigDecimal = total - forSale(saleId)
}

data class NewStockReservation(
    val saleId: UUID,
    val saleLineId: UUID,
    val locationProductId: UUID,
    val quantityReserved: BigDecimal,
)
