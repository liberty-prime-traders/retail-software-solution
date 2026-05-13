package me.ezra_home.retail_software_solution.locations.business.sale_discount.api

import me.ezra_home.retail_software_solution.locations.business.sale_discount.SaleDiscountEntity
import me.ezra_home.retail_software_solution.locations.business.sale_discount.SaleDiscountRepository
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SaleDiscountFetcher(
    private val saleDiscountRepository: SaleDiscountRepository,
    private val userQualifier: UserQualifier
) {

    fun getDiscountSummaries(saleId: UUID): List<SaleDiscountSummaryDto> =
        saleDiscountRepository.findBySaleId(saleId).map { entity ->
            SaleDiscountSummaryDto(
                saleLineId = entity.saleLineId,
                calculatedAmount = entity.calculatedAmount
            )
        }

    fun getDiscountsBySaleId(saleId: UUID): List<SaleDiscountResponseDto> =
        saleDiscountRepository.findBySaleId(saleId).map { toResponseDto(it) }

    fun getDiscountsBySaleIds(saleIds: List<UUID>): Map<UUID, List<SaleDiscountResponseDto>> =
        saleDiscountRepository.findBySaleIdIn(saleIds)
            .groupBy { it.saleId }
            .mapValues { (_, entities) -> entities.map { toResponseDto(it) } }

    private fun toResponseDto(entity: SaleDiscountEntity) = SaleDiscountResponseDto(
        id = entity.id!!,
        saleLineId = entity.saleLineId,
        calculationMethod = entity.discountType,
        value = entity.value,
        calculatedAmount = entity.calculatedAmount,
        description = entity.description,
        approvedById = entity.approvedById,
        approvedBy = userQualifier.getUserFullName(entity.approvedById)
    )
}
