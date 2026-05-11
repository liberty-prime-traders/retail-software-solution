package me.ezra_home.retail_software_solution.locations.business.sale_discount.api

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineEntity
import me.ezra_home.retail_software_solution.locations.business.sale_discount.DiscountAmountCalculator
import me.ezra_home.retail_software_solution.locations.business.sale_discount.SaleDiscountEntity
import me.ezra_home.retail_software_solution.locations.business.sale_discount.SaleDiscountRepository
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.CalculationMethod
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SaleDiscountReconciler(
    private val saleDiscountRepository: SaleDiscountRepository,
    private val saleDiscountValidator: SaleDiscountValidator,
) {

    fun reconcileDiscountsAfterLineChanges(
        saleId: UUID,
        lines: List<SaleLineEntity>,
        productSummaries: Map<UUID, LocationProductSummaryDto>,
    ) {
        val existing = saleDiscountRepository.findBySaleId(saleId)
        if (existing.isEmpty()) return
        val productByLineId = lines.filter { it.id != null }.associate { it.id!! to it.locationProductId }
        val effectiveDiscounts = replaceStalePercentageDiscounts(saleId, existing, lines, productByLineId)
        saleDiscountValidator.guardDiscountsStillFitAfterLineChanges(effectiveDiscounts, lines, productSummaries)
    }

    private fun replaceStalePercentageDiscounts(
        saleId: UUID,
        existing: List<SaleDiscountEntity>,
        lines: List<SaleLineEntity>,
        productByLineId: Map<UUID, UUID>,
    ): List<SaleDiscountEntity> {
        val stale = existing
            .filter { it.discountType == CalculationMethod.PERCENTAGE }
            .mapNotNull { entity ->
                val productId = entity.saleLineId?.let { productByLineId[it] }
                val pseudoDto = SaleDiscountCreateDto(
                    locationProductId = productId,
                    calculationMethod = entity.discountType,
                    value = entity.value,
                    description = entity.description,
                    approvedById = entity.approvedById,
                )
                val freshAmount = DiscountAmountCalculator.calculateAmount(pseudoDto, lines)
                if (freshAmount.compareTo(entity.calculatedAmount) == 0) null else entity to freshAmount
            }
        if (stale.isEmpty()) return existing
        val staleEntities = stale.map { it.first }
        val staleIds = staleEntities.mapTo(HashSet()) { it.id!! }
        saleDiscountRepository.deleteAll(staleEntities)
        val recreated = stale.map { (old, fresh) ->
            SaleDiscountEntity(
                saleId = saleId,
                saleLineId = old.saleLineId,
                discountType = old.discountType,
                value = old.value,
                calculatedAmount = fresh,
                description = old.description,
                approvedById = old.approvedById,
            )
        }
        saleDiscountRepository.saveAll(recreated)
        return existing.filter { it.id !in staleIds } + recreated
    }
}
