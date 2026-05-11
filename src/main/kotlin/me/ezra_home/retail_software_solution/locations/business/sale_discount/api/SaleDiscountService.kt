package me.ezra_home.retail_software_solution.locations.business.sale_discount.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.sale.SaleEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineEntity
import me.ezra_home.retail_software_solution.locations.business.sale_discount.DiscountAmountCalculator
import me.ezra_home.retail_software_solution.locations.business.sale_discount.SaleDiscountEntity
import me.ezra_home.retail_software_solution.locations.business.sale_discount.SaleDiscountRepository
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class SaleDiscountService(
    private val saleDiscountRepository: SaleDiscountRepository,
    private val userQualifier: UserQualifier
) {

    fun applyDiscounts(
        sale: SaleEntity,
        discountDtos: List<SaleDiscountCreateDto>,
        saleLines: List<SaleLineEntity>,
        existingDiscounts: List<SaleDiscountEntity> = emptyList()
    ): List<SaleDiscountSummaryDto> {
        if (discountDtos.isEmpty()) return emptyList()
        val productByLineId = saleLines.filter { it.id != null }.associate { it.id!! to it.locationProductId }
        SaleDiscountValidator.validateNewDiscounts(discountDtos, saleLines, existingDiscounts, productByLineId)
        val lineByProductId = saleLines.associateBy { it.locationProductId }
        val newDiscounts = discountDtos.associate { dto ->
            val saleLineId = dto.locationProductId?.let { lineByProductId[it]?.id }
            val calculatedAmount = DiscountAmountCalculator.calculateAmount(dto, saleLines)

            SaleDiscountSummaryDto(saleLineId = saleLineId, calculatedAmount = calculatedAmount) to

            SaleDiscountEntity(
                saleId = sale.id!!,
                saleLineId = saleLineId,
                discountType = dto.calculationMethod,
                value = dto.value,
                calculatedAmount = calculatedAmount,
                description = dto.description,
                approvedById = dto.approvedById
            )
        }
        saleDiscountRepository.saveAll(newDiscounts.values)
        return newDiscounts.keys.toList()
    }

    fun addDiscounts(
        sale: SaleEntity,
        discountDtos: List<SaleDiscountCreateDto>,
        lines: List<SaleLineEntity>
    ): List<SaleDiscountSummaryDto> {
        SaleDiscountValidator.guardIsDraft(sale)
        val existing = saleDiscountRepository.findBySaleId(sale.id!!)
        val newSummaries = applyDiscounts(sale, discountDtos, lines, existing)
        val existingSummaries = existing.map {
            SaleDiscountSummaryDto(saleLineId = it.saleLineId, calculatedAmount = it.calculatedAmount)
        }
        return existingSummaries + newSummaries
    }

    fun removeDiscounts(sale: SaleEntity, discountIds: List<UUID>) {
        SaleDiscountValidator.guardIsDraft(sale)
        if (discountIds.isEmpty()) return
        saleDiscountRepository.deleteByIdIn(discountIds)
    }

    fun removeDiscountsByLineIds(saleLineIds: Collection<UUID>) {
        saleDiscountRepository.deleteBySaleLineIdIn(saleLineIds)
    }

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
