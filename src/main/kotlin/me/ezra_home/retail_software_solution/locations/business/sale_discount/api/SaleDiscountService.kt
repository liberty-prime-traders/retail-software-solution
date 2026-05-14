package me.ezra_home.retail_software_solution.locations.business.sale_discount.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineSummaryDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale_discount.DiscountAmountCalculator
import me.ezra_home.retail_software_solution.locations.business.sale_discount.SaleDiscountEntity
import me.ezra_home.retail_software_solution.locations.business.sale_discount.SaleDiscountRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class SaleDiscountService(
    private val saleDiscountRepository: SaleDiscountRepository,
    private val newSaleDiscountValidator: NewSaleDiscountValidator
) {

    fun applyValidatedDiscounts(
        saleId: UUID,
        discountDtos: List<SaleDiscountCreateDto>,
        saleLines: List<SaleLineSummaryDto>
    ): List<SaleDiscountSummaryDto> {
        if (discountDtos.isEmpty()) return emptyList()
        val lineByProductId = saleLines.associateBy { it.locationProductId }
        val newDiscounts = discountDtos.associate { dto ->
            val saleLineId = dto.locationProductId?.let { lineByProductId[it]?.id }
            val calculatedAmount = DiscountAmountCalculator.calculateAmount(dto, saleLines)

            SaleDiscountSummaryDto(saleLineId = saleLineId, calculatedAmount = calculatedAmount) to

            SaleDiscountEntity(
                saleId = saleId,
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
        saleId: UUID,
        saleStatus: SaleStatus,
        existing: List<SaleDiscountEntity>,
        discountDtos: List<SaleDiscountCreateDto>,
        lines: List<SaleLineSummaryDto>,
        productSummaries: Map<UUID, LocationProductSummaryDto>,
    ): List<SaleDiscountSummaryDto> {
        SaleDiscountValidator.guardIsDraft(saleStatus)
        val existingSummaries = existing.map {
            SaleDiscountSummaryDto(saleLineId = it.saleLineId, calculatedAmount = it.calculatedAmount)
        }
        if (discountDtos.isEmpty()) return existingSummaries
        val productByLineId = lines.filter { it.id != null }.associate { it.id!! to it.locationProductId }
        newSaleDiscountValidator.validateNewDiscounts(discountDtos, lines, productSummaries, existing, productByLineId)
        val newSummaries = applyValidatedDiscounts(saleId, discountDtos, lines)
        return existingSummaries + newSummaries
    }

    fun removeDiscounts(saleStatus: SaleStatus, discountIds: List<UUID>) {
        SaleDiscountValidator.guardIsDraft(saleStatus)
        if (discountIds.isEmpty()) return
        val entities = saleDiscountRepository.findAllById(discountIds)
        saleDiscountRepository.deleteAll(entities)
    }

    fun removeDiscountsByLineIds(saleStatus: SaleStatus, saleLineIds: Collection<UUID>) {
        SaleDiscountValidator.guardIsDraft(saleStatus)
        if (saleLineIds.isEmpty()) return
        val entities = saleDiscountRepository.findBySaleLineIdIn(saleLineIds)
        saleDiscountRepository.deleteAll(entities)
    }

    fun findBySaleId(saleId: UUID): List<SaleDiscountEntity> =
        saleDiscountRepository.findBySaleId(saleId)

}
