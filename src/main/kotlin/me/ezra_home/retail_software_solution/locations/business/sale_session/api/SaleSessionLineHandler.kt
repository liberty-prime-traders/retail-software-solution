package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import java.math.BigDecimal
import java.util.UUID

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionLineAdjustmentReconciler
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionLineChangeContext
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionLineChangeContextBuilder
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionStockOverlay
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionUpdateFinalizer
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionStore
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionValidator
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockReserver
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

@Service
class SaleSessionLineHandler(
    private val saleSessionStore: SaleSessionStore,
    private val saleSessionValidator: SaleSessionValidator,
    private val saleSessionUpdateFinalizer: SaleSessionUpdateFinalizer,
    private val saleSessionLineChangeContextBuilder: SaleSessionLineChangeContextBuilder,
    private val saleSessionStockOverlay: SaleSessionStockOverlay,
    private val stockReserver: StockReserver,
    private val saleSessionLineAdjustmentReconciler: SaleSessionLineAdjustmentReconciler,
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun applyLineChanges(sessionId: UUID, lineRequestDto: SaleSessionLineRequestDto): SaleSessionResponseDto {
        val saleSession = saleSessionStore.load(sessionId)
        saleSessionValidator.guardMutable(saleSession)
        saleSessionValidator.guardNonNegativeEffectivePrices(lineRequestDto.updates)

        val saleSessionLinesByKey = saleSession.saleLines.associateBy { it.identity.key() }
        guardUpdatesTargetExistingLines(lineRequestDto.updates, saleSessionLinesByKey)

        val saleSessionLineChangeContext = saleSessionLineChangeContextBuilder.buildContext(
            additions = lineRequestDto.additions,
            updates = lineRequestDto.updates,
            saleSessionLinesByKey = saleSessionLinesByKey,
        )

        val newSaleSessionLines = buildNewSaleSessionLines(lineRequestDto.additions, saleSessionLineChangeContext)
        val mutatedExistingLines = applyUpdatesToExistingLines(
            existingSaleLines = saleSession.saleLines,
            updates = lineRequestDto.updates,
            saleSessionLineChangeContext = saleSessionLineChangeContext,
        )
        val effectiveSaleLines = buildList {
            addAll(newSaleSessionLines)
            addAll(mutatedExistingLines)
        }
        val effectiveSaleLinesByKey = effectiveSaleLines.associateBy { it.identity.key() }
        val updatedSaleAdjustments = saleSessionLineAdjustmentReconciler.reconcile(
            preUpdateSaleAdjustments = saleSession.saleAdjustments,
            preUpdateSaleSessionLinesByKey = saleSessionLinesByKey,
            postUpdateSaleSessionLinesByKey = effectiveSaleLinesByKey,
            updates = lineRequestDto.updates,
        )
        val updatedSaleSession = saleSession.copy(
            saleLines = effectiveSaleLines,
            saleAdjustments = updatedSaleAdjustments,
            productsVersion = saleSession.productsVersion + 1,
        )
        return saleSessionUpdateFinalizer.finalize(saleSessionStockOverlay.populate(updatedSaleSession))
    }

    private fun guardUpdatesTargetExistingLines(
        updates: List<SaleSessionLineUpdateDto>,
        saleSessionLinesByKey: Map<UUID, SaleSessionLine>,
    ) {
        updates.map { it.identity.key() }
            .filterNot { saleSessionLinesByKey.containsKey(it) }
            .let {
                if (it.isNotEmpty()) {
                    throw RtsGenericException("Attempted to update a line that is not in the session")
                }
            }
    }

    private fun buildNewSaleSessionLines(
        additions: List<SaleSessionLineAddDto>,
        saleSessionLineChangeContext: SaleSessionLineChangeContext,
    ): List<SaleSessionLine> {

        return additions.map { lineAddDto ->
            val productSummary = saleSessionLineChangeContext.locationProductSummariesById.getValue(lineAddDto.locationProductId)
            val defaultSalePrice = saleSessionLineChangeContext.defaultSalePricesByLocationProductId[lineAddDto.locationProductId]
                ?: throw RtsGenericException("Product ${productSummary.label} has no default sale price")
            SaleSessionLine(
                identity = SessionIdentity.mintFreshIdentity(),
                locationProductId = lineAddDto.locationProductId,
                productLabel = productSummary.label,
                quantity = lineAddDto.quantity,
                unitId = productSummary.baseUnitId,
                baseUnitId = productSummary.baseUnitId,
                conversionFactor = BigDecimal.ONE,
                defaultSalePrice = defaultSalePrice,
            )
        }
    }

    private fun applyUpdatesToExistingLines(
        existingSaleLines: List<SaleSessionLine>,
        updates: List<SaleSessionLineUpdateDto>,
        saleSessionLineChangeContext: SaleSessionLineChangeContext,
    ): List<SaleSessionLine> {
        val updatesByLineKey = updates.associateBy { it.identity.key() }
        return existingSaleLines.map { saleSessionLine ->
            val lineUpdateDto = updatesByLineKey[saleSessionLine.identity.key()] ?: return@map saleSessionLine
            val conversionFactor = saleSessionLineChangeContext.newConversionFactorsByLineKey[saleSessionLine.identity.key()]
                ?: saleSessionLine.conversionFactor
            saleSessionLine.copy(
                quantity = lineUpdateDto.quantity,
                unitId = lineUpdateDto.unitId,
                conversionFactor = conversionFactor,
            )
        }
    }

    @TransactionalOnLocationSchema
    fun removeLine(sessionId: UUID, rowIdentityDto: SaleSessionRowIdentityDto): SaleSessionResponseDto {
        val saleSession = saleSessionStore.load(sessionId)
        saleSessionValidator.guardMutable(saleSession)
        val targetLineKey = rowIdentityDto.identity.key()
        val removedSaleSessionLine = saleSession.saleLines.firstOrNull { it.identity.key() == targetLineKey }
            ?: throw RtsGenericException("Line not found on session")
        val persistedSaleLineId = removedSaleSessionLine.identity.id
        if (persistedSaleLineId != null) {
            stockReserver.clearBySaleLineIds(listOf(persistedSaleLineId))
        }
        val survivingSaleLines = saleSession.saleLines.filter { it.identity.key() != targetLineKey }
        val survivingSaleAdjustments = saleSession.saleAdjustments.filter { saleSessionAdjustment ->
            saleSessionAdjustment.relatedSaleLineIdentity == null ||
                    saleSessionAdjustment.relatedSaleLineIdentity.key() != targetLineKey
        }
        val updatedSaleSession = saleSession.copy(
            saleLines = survivingSaleLines,
            saleAdjustments = survivingSaleAdjustments,
            productsVersion = saleSession.productsVersion + 1,
        )
        return saleSessionUpdateFinalizer.finalize(saleSessionStockOverlay.populate(updatedSaleSession))
    }
}
