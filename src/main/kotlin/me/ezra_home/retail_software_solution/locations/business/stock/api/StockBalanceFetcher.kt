package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class StockBalanceFetcher(private val stockMovementRepository: StockMovementRepository) {

    fun getLatestBalances(locationProductIds: List<UUID>): Map<UUID, BigDecimal> =
        stockMovementRepository.findLatestBalances(locationProductIds)
            .associate { it.getLocationProductId() to it.getRemainingQuantity() }
}
