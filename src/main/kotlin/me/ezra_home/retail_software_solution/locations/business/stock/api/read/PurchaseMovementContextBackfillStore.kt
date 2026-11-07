package me.ezra_home.retail_software_solution.locations.business.stock.api.read

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockMovementContexts
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import org.springframework.stereotype.Service
import java.util.UUID

// Short-lived: deleted with the rest of the purchase movement context backfill once every organization has run it.
@Service
@TransactionalOnLocationSchema
class PurchaseMovementContextBackfillStore(
    private val stockMovementRepository: StockMovementRepository,
) {

    fun findMovementsMissingContext(
        afterStockMovementId: UUID,
        batchSize: Int
    ): List<PurchaseMovementContextCandidate> =
        stockMovementRepository.findMovementsOfTypeMissingPurchaseContext(
            MovementType.PURCHASE_RECEIVED.code,
            afterStockMovementId,
            batchSize
        )
            .map { PurchaseMovementContextCandidate(it.stockMovementId, it.purchaseReferenceNumber) }

    // Loaded and saved as entities, not updated in bulk, so the audit table records each change
    fun applyContexts(candidates: List<PurchaseMovementContextCandidate>) {
        val purchaseReferenceNumberByMovementId =
            candidates.associate { it.stockMovementId to it.purchaseReferenceNumber }
        val stockMovements = stockMovementRepository.findAllById(purchaseReferenceNumberByMovementId.keys)
        stockMovements.forEach { it.context = StockMovementContexts.purchase(
            purchaseReferenceNumberByMovementId.getValue(it.id!!)
        ) }
        stockMovementRepository.saveAllAndFlush(stockMovements)
    }

    fun countMovementsMissingContext(): Long =
        stockMovementRepository.countMovementsOfTypeMissingPurchaseContext(MovementType.PURCHASE_RECEIVED.code)
}
