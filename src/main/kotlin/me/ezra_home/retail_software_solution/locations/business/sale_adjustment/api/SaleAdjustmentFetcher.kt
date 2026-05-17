package me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api

import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.SaleAdjustmentEntity
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.SaleAdjustmentRepository
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SaleAdjustmentFetcher(
    private val saleAdjustmentRepository: SaleAdjustmentRepository,
    private val userQualifier: UserQualifier,
) {

    fun getAdjustmentSummaries(saleId: UUID): List<SaleAdjustmentSummaryDto> =
        saleAdjustmentRepository.findBySaleId(saleId).map { entity ->
            SaleAdjustmentSummaryDto(
                saleLineId = entity.saleLineId,
                direction = entity.direction,
                calculatedAmount = entity.calculatedAmount,
            )
        }

    fun getAdjustmentsBySaleId(saleId: UUID): List<SaleAdjustmentResponseDto> =
        saleAdjustmentRepository.findBySaleId(saleId).map { toResponseDto(it) }

    fun getAdjustmentsBySaleIds(saleIds: List<UUID>): Map<UUID, List<SaleAdjustmentResponseDto>> =
        saleAdjustmentRepository.findBySaleIdIn(saleIds)
            .groupBy { it.saleId }
            .mapValues { (_, entities) -> entities.map { toResponseDto(it) } }

    private fun toResponseDto(entity: SaleAdjustmentEntity) = SaleAdjustmentResponseDto(
        id = entity.id!!,
        saleLineId = entity.saleLineId,
        direction = entity.direction,
        calculationMethod = entity.calculationMethod,
        value = entity.value,
        calculatedAmount = entity.calculatedAmount,
        adjustmentReasonId = entity.adjustmentReasonId,
        note = entity.note,
        approvedById = entity.approvedById,
        approvedBy = userQualifier.getUserFullName(entity.approvedById),
    )
}
