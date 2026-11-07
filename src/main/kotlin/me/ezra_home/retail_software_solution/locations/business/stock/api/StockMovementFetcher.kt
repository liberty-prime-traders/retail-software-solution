package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema(readOnly = true)
class StockMovementFetcher(
    private val stockMovementRepository: StockMovementRepository,
) {

    fun findOnEntry(stockEntryId: UUID): List<StockMovementDto> =
        stockMovementRepository.findByStockEntryId(stockEntryId).map { StockMovementDto.of(it) }

    fun findOfDocuments(locationProductId: UUID, documentReferences: Set<String>): List<StockMovementDto> =
        stockMovementRepository.findByLocationProductIdAndDocumentReferenceIn(locationProductId, documentReferences)
            .map { StockMovementDto.of(it) }
}
