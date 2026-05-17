package me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineSummaryDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.AdjustmentAmountCalculator
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.SaleAdjustmentEntity
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.SaleAdjustmentRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class SaleAdjustmentService(
    private val saleAdjustmentRepository: SaleAdjustmentRepository,
    private val newSaleAdjustmentValidator: NewSaleAdjustmentValidator,
) {

    fun applyValidatedAdjustments(
        saleId: UUID,
        adjustmentDtos: List<SaleAdjustmentCreateDto>,
        saleLines: List<SaleLineSummaryDto>,
    ): List<SaleAdjustmentSummaryDto> {
        if (adjustmentDtos.isEmpty()) return emptyList()
        val lineByProductId = saleLines.associateBy { it.locationProductId }
        val newAdjustments = adjustmentDtos.associate { dto ->
            val saleLineId = dto.locationProductId?.let { lineByProductId[it]?.id }
            val calculatedAmount = AdjustmentAmountCalculator.calculateAmount(dto, saleLines)

            SaleAdjustmentSummaryDto(
                saleLineId = saleLineId,
                direction = dto.direction,
                calculatedAmount = calculatedAmount,
            ) to SaleAdjustmentEntity(
                saleId = saleId,
                saleLineId = saleLineId,
                direction = dto.direction,
                calculationMethod = dto.calculationMethod,
                value = dto.value,
                calculatedAmount = calculatedAmount,
                adjustmentReasonId = dto.adjustmentReasonId,
                note = dto.note,
                approvedById = dto.approvedById,
            )
        }
        saleAdjustmentRepository.saveAll(newAdjustments.values)
        return newAdjustments.keys.toList()
    }

    fun addAdjustments(
        saleId: UUID,
        saleStatus: SaleStatus,
        existing: List<SaleAdjustmentEntity>,
        adjustmentDtos: List<SaleAdjustmentCreateDto>,
        lines: List<SaleLineSummaryDto>,
        productSummaries: Map<UUID, LocationProductSummaryDto>,
    ): List<SaleAdjustmentSummaryDto> {
        SaleAdjustmentValidator.guardIsDraft(saleStatus)
        val existingSummaries = existing.map {
            SaleAdjustmentSummaryDto(
                saleLineId = it.saleLineId,
                direction = it.direction,
                calculatedAmount = it.calculatedAmount,
            )
        }
        if (adjustmentDtos.isEmpty()) return existingSummaries
        val productByLineId = lines.filter { it.id != null }.associate { it.id!! to it.locationProductId }
        newSaleAdjustmentValidator.validateNewAdjustments(
            adjustmentDtos, lines, productSummaries, existing, productByLineId,
        )
        val newSummaries = applyValidatedAdjustments(saleId, adjustmentDtos, lines)
        return existingSummaries + newSummaries
    }

    fun removeAdjustments(saleStatus: SaleStatus, adjustmentIds: List<UUID>) {
        SaleAdjustmentValidator.guardIsDraft(saleStatus)
        if (adjustmentIds.isEmpty()) return
        val entities = saleAdjustmentRepository.findAllById(adjustmentIds)
        saleAdjustmentRepository.deleteAll(entities)
    }

    fun removeAdjustmentsByLineIds(saleStatus: SaleStatus, saleLineIds: Collection<UUID>) {
        SaleAdjustmentValidator.guardIsDraft(saleStatus)
        if (saleLineIds.isEmpty()) return
        val entities = saleAdjustmentRepository.findBySaleLineIdIn(saleLineIds)
        saleAdjustmentRepository.deleteAll(entities)
    }

    fun findBySaleId(saleId: UUID): List<SaleAdjustmentEntity> =
        saleAdjustmentRepository.findBySaleId(saleId)
}
