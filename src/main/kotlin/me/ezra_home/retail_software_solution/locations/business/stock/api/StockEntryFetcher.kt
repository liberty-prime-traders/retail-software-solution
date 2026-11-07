package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.stockEntryFifoComparator
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema(readOnly = true)
class StockEntryFetcher(
    private val stockEntryRepository: StockEntryRepository,
) {

    fun get(stockEntryId: UUID): StockEntryDto =
        StockEntryDto.of(stockEntryRepository.findById(stockEntryId).orElseThrow { RtsGenericException("Stock entry $stockEntryId not found.") })

    fun findWithStockLeftInDrawOrder(locationProductId: UUID): List<StockEntryFifoDto> =
        stockEntryRepository.findFifoEntriesForProducts(listOf(locationProductId))
            .sortedWith(stockEntryFifoComparator)
            .map { it.toFifoDto() }
}
