package me.ezra_home.retail_software_solution.locations.business.sale_discount.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.sale.SaleEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineEntity
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
        sale: SaleEntity,
        discountDtos: List<SaleDiscountCreateDto>,
        saleLines: List<SaleLineEntity>
    ): List<SaleDiscountSummaryDto> {
        if (discountDtos.isEmpty()) return emptyList()
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
        existing: List<SaleDiscountEntity>,
        discountDtos: List<SaleDiscountCreateDto>,
        lines: List<SaleLineEntity>,
        productSummaries: Map<UUID, LocationProductSummaryDto>,
    ): List<SaleDiscountSummaryDto> {
        SaleDiscountValidator.guardIsDraft(sale)
        val existingSummaries = existing.map {
            SaleDiscountSummaryDto(saleLineId = it.saleLineId, calculatedAmount = it.calculatedAmount)
        }
        if (discountDtos.isEmpty()) return existingSummaries
        val productByLineId = lines.filter { it.id != null }.associate { it.id!! to it.locationProductId }
        newSaleDiscountValidator.validateNewDiscounts(discountDtos, lines, productSummaries, existing, productByLineId)
        val newSummaries = applyValidatedDiscounts(sale, discountDtos, lines)
        return existingSummaries + newSummaries
    }

    fun removeDiscounts(sale: SaleEntity, discountIds: List<UUID>) {
        SaleDiscountValidator.guardIsDraft(sale)
        if (discountIds.isEmpty()) return
        val entities = saleDiscountRepository.findAllById(discountIds)
        saleDiscountRepository.deleteAll(entities)
    }

    fun removeDiscountsByLineIds(saleLineIds: Collection<UUID>) {
        if (saleLineIds.isEmpty()) return
        val entities = saleDiscountRepository.findBySaleLineIdIn(saleLineIds)
        saleDiscountRepository.deleteAll(entities)
    }

}
