package me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api

import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.SaleAdjustmentEntity
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.SaleAdjustmentRepository
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.CalculationMethod
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import org.springframework.stereotype.Service
import java.math.BigDecimal
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

    fun getAdjustmentSnapshots(saleId: UUID): List<SaleAdjustmentSnapshot> =
        saleAdjustmentRepository.findBySaleId(saleId).map { entity ->
            SaleAdjustmentSnapshot(
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

data class SaleAdjustmentSnapshot(
    val id: UUID,
    val saleLineId: UUID?,
    val adjustmentReasonId: UUID,
    val direction: AdjustmentDirection,
    val calculationMethod: CalculationMethod,
    val value: BigDecimal,
    val note: String?,
    val approvedById: UUID?,
)
