package me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api

import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductLineWithPrice
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleAdjustmentSaveRequest
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleSaveRequest
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.SaleAdjustmentEntity
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.SaleAdjustmentRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

data class PersistedSaleLine(
    val id: UUID,
    override val locationProductId: UUID,
    override val quantity: BigDecimal,
    override val unitPrice: BigDecimal,
) : ProductLineWithPrice

@Service
class SaleAdjustmentSyncer(
    private val saleAdjustmentRepository: SaleAdjustmentRepository,
) {

    fun sync(
        saleId: UUID,
        saleSaveRequest: SaleSaveRequest,
        persistedSaleLines: List<PersistedSaleLine>,
        saleLineIdsByClientKey: Map<UUID, UUID>,
    ): Map<UUID, UUID> {
        val existingSaleAdjustmentsById = saleAdjustmentRepository.findBySaleId(saleId).associateBy { it.id!! }
        val incomingSaleAdjustmentIds = saleSaveRequest.saleAdjustments.mapNotNull { it.existingId }.toHashSet()
        val saleAdjustmentsToDelete = existingSaleAdjustmentsById.values.filter { it.id !in incomingSaleAdjustmentIds }
        if (saleAdjustmentsToDelete.isNotEmpty()) {
            saleAdjustmentRepository.deleteAll(saleAdjustmentsToDelete)
        }

        val persistedSaleLineByLocationProductId = persistedSaleLines.associateBy { it.locationProductId }
        val saleAdjustmentIdsByClientKey = mutableMapOf<UUID, UUID>()
        val saleAdjustmentsToUpdate = mutableListOf<SaleAdjustmentEntity>()
        val saleAdjustmentsToCreate = mutableListOf<Pair<UUID, SaleAdjustmentEntity>>()

        saleSaveRequest.saleAdjustments.forEach { saleAdjustmentSaveRequest ->
            val locationProductId = saleAdjustmentSaveRequest.relatedSaleLineClientKey?.let { relatedSaleLineClientKey ->
                val saleLineId = saleLineIdsByClientKey.getValue(relatedSaleLineClientKey)
                persistedSaleLines.firstOrNull { it.id == saleLineId }?.locationProductId
            }
            val saleLineId = locationProductId?.let { persistedSaleLineByLocationProductId[it]?.id }
            val saleAdjustmentCreateDto = toCreatableAdjustment(saleAdjustmentSaveRequest, locationProductId)
            val calculatedAmount = AdjustmentAmountCalculator.calculateAmount(saleAdjustmentCreateDto, persistedSaleLines)
            if (saleAdjustmentSaveRequest.existingId != null) {
                val keptSaleAdjustmentEntity = existingSaleAdjustmentsById[saleAdjustmentSaveRequest.existingId]
                    ?: throw IllegalStateException("Sale adjustment ${saleAdjustmentSaveRequest.existingId} no longer exists")
                keptSaleAdjustmentEntity.saleLineId = saleLineId
                keptSaleAdjustmentEntity.calculatedAmount = calculatedAmount
                saleAdjustmentsToUpdate.add(keptSaleAdjustmentEntity)
                saleAdjustmentIdsByClientKey[saleAdjustmentSaveRequest.clientKey] = saleAdjustmentSaveRequest.existingId
            } else {
                saleAdjustmentsToCreate.add(
                    saleAdjustmentSaveRequest.clientKey to SaleAdjustmentEntity(
                        saleId = saleId,
                        saleLineId = saleLineId,
                        direction = saleAdjustmentSaveRequest.direction,
                        calculationMethod = saleAdjustmentSaveRequest.calculationMethod,
                        value = saleAdjustmentSaveRequest.value,
                        calculatedAmount = calculatedAmount,
                        adjustmentReasonId = saleAdjustmentSaveRequest.adjustmentReasonId,
                        note = saleAdjustmentSaveRequest.note,
                        approvedById = saleAdjustmentSaveRequest.approvedById,
                    )
                )
            }
        }

        if (saleAdjustmentsToUpdate.isNotEmpty()) saleAdjustmentRepository.saveAll(saleAdjustmentsToUpdate)
        if (saleAdjustmentsToCreate.isNotEmpty()) {
            val savedSaleAdjustments = saleAdjustmentRepository.saveAll(saleAdjustmentsToCreate.map { it.second }).toList()
            saleAdjustmentsToCreate.forEachIndexed { saleAdjustmentIndex, (clientKey, _) ->
                saleAdjustmentIdsByClientKey[clientKey] = savedSaleAdjustments[saleAdjustmentIndex].id!!
            }
        }
        return saleAdjustmentIdsByClientKey
    }

    private fun toCreatableAdjustment(
        saleAdjustmentSaveRequest: SaleAdjustmentSaveRequest,
        locationProductId: UUID?,
    ): SaleAdjustmentCreateDto {
        return SaleAdjustmentCreateDto(
            locationProductId = locationProductId,
            direction = saleAdjustmentSaveRequest.direction,
            calculationMethod = saleAdjustmentSaveRequest.calculationMethod,
            value = saleAdjustmentSaveRequest.value,
            adjustmentReasonId = saleAdjustmentSaveRequest.adjustmentReasonId,
            note = saleAdjustmentSaveRequest.note,
            approvedById = saleAdjustmentSaveRequest.approvedById,
        )
    }
}
