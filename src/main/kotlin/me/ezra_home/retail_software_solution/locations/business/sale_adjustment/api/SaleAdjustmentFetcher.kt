package me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api

import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.SaleAdjustmentRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SaleAdjustmentFetcher(
    private val saleAdjustmentRepository: SaleAdjustmentRepository
) {

    fun getAdjustmentSummaries(saleId: UUID): List<SaleAdjustmentSummaryDto> =
        saleAdjustmentRepository.findBySaleId(saleId).map { entity ->
            SaleAdjustmentSummaryDto(
                saleLineId = entity.saleLineId,
                direction = entity.direction,
                calculatedAmount = entity.calculatedAmount,
            )
        }

    fun getAdjustments(saleId: UUID): List<SaleAdjustmentDto> =
        saleAdjustmentRepository.findBySaleId(saleId).map { entity ->
            SaleAdjustmentDto(
                id = entity.id!!,
                saleLineId = entity.saleLineId,
                adjustmentReasonId = entity.adjustmentReasonId,
                direction = entity.direction,
                calculationMethod = entity.calculationMethod,
                value = entity.value,
                note = entity.note,
                approvedById = entity.approvedById,
            )
        }
}
