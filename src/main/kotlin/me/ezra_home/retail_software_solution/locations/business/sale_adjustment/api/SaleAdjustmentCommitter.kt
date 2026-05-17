package me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api

import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductLineWithPrice
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleCommitInput
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.AdjustmentAmountCalculator
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.SaleAdjustmentEntity
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.SaleAdjustmentRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

data class PersistedCommitLine(
    val id: UUID,
    override val locationProductId: UUID,
    override val quantity: BigDecimal,
    override val unitPrice: BigDecimal,
) : ProductLineWithPrice

@Service
class SaleAdjustmentCommitter(
    private val saleAdjustmentRepository: SaleAdjustmentRepository,
) {

    fun sync(
        saleId: UUID,
        input: SaleCommitInput,
        persistedLines: List<PersistedCommitLine>,
        lineIdByClientKey: Map<UUID, UUID>,
    ): Map<UUID, UUID> {
        val existing = saleAdjustmentRepository.findBySaleId(saleId).associateBy { it.id!! }
        val incomingIds = input.adjustments.mapNotNull { it.existingId }.toHashSet()
        val toDelete = existing.values.filter { it.id !in incomingIds }
        if (toDelete.isNotEmpty()) {
            saleAdjustmentRepository.deleteAll(toDelete)
        }
        val existingIdByClientKey = input.adjustments
            .filter { it.existingId != null }
            .associate { it.clientKey to it.existingId!! }
        val toCreate = input.adjustments.filter { it.existingId == null }
        if (toCreate.isEmpty()) return existingIdByClientKey

        val lineByProductId = persistedLines.associateBy { it.locationProductId }
        val newPayload = toCreate.map { adj ->
            val productId = adj.lineClientKey?.let { lineClientKey ->
                val lineId = lineIdByClientKey[lineClientKey]
                    ?: throw IllegalStateException("Missing line id for adjustment client key $lineClientKey")
                persistedLines.firstOrNull { it.id == lineId }?.locationProductId
            }
            val calculatedAmount = AdjustmentAmountCalculator.calculateAmount(
                SaleAdjustmentCreateDto(
                    locationProductId = productId,
                    direction = adj.direction,
                    calculationMethod = adj.calculationMethod,
                    value = adj.value,
                    adjustmentReasonId = adj.adjustmentReasonId,
                    note = adj.note,
                    approvedById = adj.approvedById,
                ),
                persistedLines,
            )
            adj to SaleAdjustmentEntity(
                saleId = saleId,
                saleLineId = productId?.let { lineByProductId[it]?.id },
                direction = adj.direction,
                calculationMethod = adj.calculationMethod,
                value = adj.value,
                calculatedAmount = calculatedAmount,
                adjustmentReasonId = adj.adjustmentReasonId,
                note = adj.note,
                approvedById = adj.approvedById,
            )
        }
        val saved = saleAdjustmentRepository.saveAll(newPayload.map { it.second }).toList()
        val newIdByClientKey = newPayload.mapIndexed { index, pair ->
            pair.first.clientKey to saved[index].id!!
        }.toMap()
        return newIdByClientKey + existingIdByClientKey
    }
}
