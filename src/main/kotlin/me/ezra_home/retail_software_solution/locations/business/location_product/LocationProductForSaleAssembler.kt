package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductForSaleDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBalanceFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockReserver
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class LocationProductForSaleAssembler(
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val stockReserver: StockReserver,
) {

    fun assemble(activeProducts: List<LocationProductDto>): List<LocationProductForSaleDto> {
        val sellable = activeProducts.filter { it.defaultSalePrice != null }
        val sellableProductIds = sellable.map { it.id }
        val quantityOnHandByProductId = stockBalanceFetcher.getLatestBalances(sellableProductIds)
        val reservationsByProductId = stockReserver.loadReservationBreakdown(sellableProductIds)
        return sellable.map { locationProductDto ->
            val quantityOnHand = quantityOnHandByProductId[locationProductDto.id] ?: BigDecimal.ZERO
            val quantityReserved = reservationsByProductId[locationProductDto.id]?.total ?: BigDecimal.ZERO
            LocationProductForSaleDto(
                id = locationProductDto.id,
                referenceNumber = locationProductDto.referenceNumber,
                productName = locationProductDto.productName!!,
                productGroupName = locationProductDto.productGroupName!!,
                defaultSalePrice = locationProductDto.defaultSalePrice!!,
                quantityOnHand = quantityOnHand,
                quantityReserved = quantityReserved,
                quantityAvailable = quantityOnHand - quantityReserved,
            )
        }
    }
}
