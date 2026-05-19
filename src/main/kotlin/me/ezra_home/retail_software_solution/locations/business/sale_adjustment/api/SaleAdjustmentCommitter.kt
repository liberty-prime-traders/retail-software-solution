package me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api

import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductLineWithPrice
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleCommitAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleCommitInput
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
        saleLineIdByClientKey: Map<UUID, UUID>,
    ): Map<UUID, UUID> {
        val existing = saleAdjustmentRepository.findBySaleId(saleId).associateBy { it.id!! }
        val incomingIds = input.adjustments.mapNotNull { it.existingId }.toHashSet()
        val toDelete = existing.values.filter { it.id !in incomingIds }
        if (toDelete.isNotEmpty()) {
            saleAdjustmentRepository.deleteAll(toDelete)
        }

        val lineByProductId = persistedLines.associateBy { it.locationProductId }
        val resultByClientKey = mutableMapOf<UUID, UUID>()
        val toUpdate = mutableListOf<SaleAdjustmentEntity>()
        val toCreate = mutableListOf<Pair<UUID, SaleAdjustmentEntity>>()

        input.adjustments.forEach { adj ->
            val productId = adj.lineClientKey?.let { lineClientKey ->
                val saleLineId = saleLineIdByClientKey.getValue(lineClientKey)
                persistedLines.firstOrNull { it.id == saleLineId }?.locationProductId
            }
            val saleLineId = productId?.let { lineByProductId[it]?.id }
            val adjustment = toCreatableAdjustment(adj, productId)
            val calculatedAmount = AdjustmentAmountCalculator.calculateAmount(adjustment, persistedLines,)
            if (adj.existingId != null) {
                val kept = existing[adj.existingId]
                    ?: throw IllegalStateException("Sale adjustment ${adj.existingId} no longer exists")
                kept.saleLineId = saleLineId
                kept.calculatedAmount = calculatedAmount
                toUpdate.add(kept)
                resultByClientKey[adj.clientKey] = adj.existingId
            } else {
                toCreate.add(
                    adj.clientKey to SaleAdjustmentEntity(
                        saleId = saleId,
                        saleLineId = saleLineId,
                        direction = adj.direction,
                        calculationMethod = adj.calculationMethod,
                        value = adj.value,
                        calculatedAmount = calculatedAmount,
                        adjustmentReasonId = adj.adjustmentReasonId,
                        note = adj.note,
                        approvedById = adj.approvedById,
                    )
                )
            }
        }

        if (toUpdate.isNotEmpty()) saleAdjustmentRepository.saveAll(toUpdate)
        if (toCreate.isNotEmpty()) {
            val saved = saleAdjustmentRepository.saveAll(toCreate.map { it.second }).toList()
            toCreate.forEachIndexed { index, (clientKey, _) ->
                resultByClientKey[clientKey] = saved[index].id!!
            }
        }
        return resultByClientKey
    }

    private fun toCreatableAdjustment(adj: SaleCommitAdjustment, productId: UUID?): SaleAdjustmentCreateDto {
        return SaleAdjustmentCreateDto(
            locationProductId = productId,
            direction = adj.direction,
            calculationMethod = adj.calculationMethod,
            value = adj.value,
            adjustmentReasonId = adj.adjustmentReasonId,
            note = adj.note,
            approvedById = adj.approvedById,
        )
    }
}
