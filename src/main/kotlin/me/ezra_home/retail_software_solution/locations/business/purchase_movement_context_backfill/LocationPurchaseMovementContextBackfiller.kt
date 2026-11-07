package me.ezra_home.retail_software_solution.locations.business.purchase_movement_context_backfill

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock.api.PurchaseMovementContextBackfillStore
import org.springframework.stereotype.Service
import java.util.UUID

// Short-lived: deleted with the rest of the purchase movement context backfill once every organization has run it.
@Service
@TransactionalOnLocationSchema
class LocationPurchaseMovementContextBackfiller(
    private val purchaseMovementContextBackfillStore: PurchaseMovementContextBackfillStore,
) {

    // One call is one transaction, so the caller loops with the last id it got back until it returns null. Paging by id
    // keeps a long run from rescanning rows an earlier batch already passed.
    fun backfillNextBatch(afterStockMovementId: UUID): PurchaseMovementContextBatch? {
        val candidates = purchaseMovementContextBackfillStore.findMovementsMissingContext(afterStockMovementId, BATCH_SIZE)
        if (candidates.isEmpty()) return null
        purchaseMovementContextBackfillStore.applyContexts(candidates)
        return PurchaseMovementContextBatch(candidates.size, candidates.last().stockMovementId)
    }

    fun countsAfterUpdating(updatedMovementCount: Long) = PurchaseMovementContextBackfillCounts(
        updatedMovementCount = updatedMovementCount,
        movementsStillMissingContextCount = purchaseMovementContextBackfillStore.countMovementsMissingContext(),
    )

    private companion object {
        const val BATCH_SIZE = 500
    }
}
