package me.ezra_home.retail_software_solution.locations.business.opening_stock.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.opening_stock.OpeningStockRepository
import org.springframework.stereotype.Component
import java.util.UUID


@Component
@TransactionalOnLocationSchema(readOnly = true)
class OpeningStockAmountsFetcher(
    private val openingStockRepository: OpeningStockRepository
) {

    fun getAmountsByProductIds(locationProductIds: Collection<UUID>): Map<UUID, OpeningStockAmountsDto> {
        if (locationProductIds.isEmpty()) return emptyMap()
        return openingStockRepository.findByLocationProductIdIn(locationProductIds)
            .associate { it.locationProductId to OpeningStockAmountsDto(quantity = it.quantity, unitCost = it.unitCost) }
    }
}
