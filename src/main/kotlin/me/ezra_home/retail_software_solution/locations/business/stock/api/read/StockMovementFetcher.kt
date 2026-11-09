package me.ezra_home.retail_software_solution.locations.business.stock.api.read

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema(readOnly = true)
class StockMovementFetcher(
    private val stockMovementRepository: StockMovementRepository,
) {

    fun findOnEntry(stockEntryId: UUID): List<StockMovementDto> =
        stockMovementRepository.findByStockEntryId(stockEntryId).map { StockMovementDto.of(it) }

    fun findOfRevision(stockRevisionReference: String): List<StockMovementDto> =
        stockMovementRepository.findByStockRevisionReference(stockRevisionReference).map { StockMovementDto.of(it) }

    fun findOfDocuments(locationProductId: UUID, documentReferences: Set<String>): List<StockMovementDto> =
        stockMovementRepository.findByLocationProductIdAndDocumentReferenceIn(locationProductId, documentReferences)
            .map { StockMovementDto.of(it) }

    fun findOfDocumentsOfTypes(documentReferences: Collection<String>, movementTypes: Collection<MovementType>): List<StockMovementDto> =
        stockMovementRepository.findByDocumentReferenceInAndMovementTypeIn(documentReferences, movementTypes)
            .map { StockMovementDto.of(it) }
}
