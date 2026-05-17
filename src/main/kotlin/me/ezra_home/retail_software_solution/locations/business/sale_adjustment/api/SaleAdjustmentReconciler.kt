package me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineSummaryDto
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.AdjustmentAmountCalculator
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.SaleAdjustmentEntity
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.SaleAdjustmentRepository
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.CalculationMethod
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SaleAdjustmentReconciler(
    private val saleAdjustmentRepository: SaleAdjustmentRepository,
) {

    fun reconcileAdjustmentsAfterLineChanges(
        saleId: UUID,
        lines: List<SaleLineSummaryDto>,
    ): List<SaleAdjustmentEntity> {
        val existing = saleAdjustmentRepository.findBySaleId(saleId)
        if (existing.isEmpty()) return emptyList()
        val productByLineId = lines.filter { it.id != null }.associate { it.id!! to it.locationProductId }
        return replaceStalePercentageAdjustments(saleId, existing, lines, productByLineId)
    }

    private fun replaceStalePercentageAdjustments(
        saleId: UUID,
        existing: List<SaleAdjustmentEntity>,
        lines: List<SaleLineSummaryDto>,
        productByLineId: Map<UUID, UUID>,
    ): List<SaleAdjustmentEntity> {
        val stale = existing
            .filter { it.calculationMethod == CalculationMethod.PERCENTAGE }
            .mapNotNull { entity ->
                val productId = entity.saleLineId?.let { productByLineId[it] }
                val pseudoDto = SaleAdjustmentCreateDto(
                    locationProductId = productId,
                    direction = entity.direction,
                    calculationMethod = entity.calculationMethod,
                    value = entity.value,
                    adjustmentReasonId = entity.adjustmentReasonId,
                    note = entity.note,
                    approvedById = entity.approvedById,
                )
                val freshAmount = AdjustmentAmountCalculator.calculateAmount(pseudoDto, lines)
                if (freshAmount.compareTo(entity.calculatedAmount) == 0) null else entity to freshAmount
            }
        if (stale.isEmpty()) return existing
        val staleEntities = stale.map { it.first }
        val staleIds = staleEntities.mapTo(HashSet()) { it.id!! }
        saleAdjustmentRepository.deleteAll(staleEntities)
        val recreated = stale.map { (old, fresh) ->
            SaleAdjustmentEntity(
                saleId = saleId,
                saleLineId = old.saleLineId,
                direction = old.direction,
                calculationMethod = old.calculationMethod,
                value = old.value,
                calculatedAmount = fresh,
                adjustmentReasonId = old.adjustmentReasonId,
                note = old.note,
                approvedById = old.approvedById,
            )
        }
        saleAdjustmentRepository.saveAll(recreated)
        return existing.filter { it.id !in staleIds } + recreated
    }
}
