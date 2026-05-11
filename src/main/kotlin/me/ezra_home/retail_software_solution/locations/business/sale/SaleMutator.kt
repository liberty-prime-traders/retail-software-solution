package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductLineWithPrice
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleCreateDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineCreateDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleUpdateDto
import me.ezra_home.retail_software_solution.locations.business.sale_discount.api.SaleDiscountService
import me.ezra_home.retail_software_solution.locations.business.sale_discount.api.SaleDiscountSummaryDto
import me.ezra_home.retail_software_solution.locations.business.sale_discount.api.SaleDiscountValidator
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentCreateDto
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentService
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

data class SaleCreateOutcome(
    val lines: List<SaleLineEntity>,
    val validated: ValidatedSaleLines,
    val discounts: List<SaleDiscountSummaryDto>,
)

data class SaleUpdateOutcome(
    val updateResult: SaleLineUpdateResult,
    val discounts: List<SaleDiscountSummaryDto>,
)

private data class PricedSaleLine(
    override val locationProductId: UUID,
    override val quantity: BigDecimal,
    override val unitPrice: BigDecimal
) : ProductLineWithPrice

@Service
class SaleMutator(
    private val saleRepository: SaleRepository,
    private val saleLineRepository: SaleLineRepository,
    private val saleLinesUpdater: SaleLinesUpdater,
    private val salePaymentService: SalePaymentService,
    private val saleStockReserver: SaleStockReserver,
    private val saleDiscountService: SaleDiscountService,
) {

    fun create(dto: SaleCreateDto, sale: SaleEntity, validated: ValidatedSaleLines): SaleCreateOutcome {
        SaleDiscountValidator.validateNewDiscounts(
            dto.discounts, attachUnitPrices(dto.linesToAdd, validated.productSummaries)
        )
        saleRepository.save(sale)
        val saleLineEntities = SaleLineMapper.toLineEntities(sale.id!!, dto.linesToAdd, validated)
        saleLineRepository.saveAll(saleLineEntities)
        if (sale.status == SaleStatus.DRAFT) {
            saleStockReserver.reserve(sale.id!!, saleLineEntities)
        }
        val discounts = saleDiscountService.applyDiscounts(sale, dto.discounts, saleLineEntities)
        applyTotals(sale, saleLineEntities, discounts)
        recordPayments(dto.payments, sale)
        return SaleCreateOutcome(saleLineEntities, validated, discounts)
    }

    private fun attachUnitPrices(
        dtoLines: List<SaleLineCreateDto>,
        productSummaries: Map<UUID, LocationProductSummaryDto>,
    ): List<ProductLineWithPrice> = dtoLines.map { line ->
        val unitPrice = productSummaries.getValue(line.locationProductId).unitPrice
            ?: throw RtsGenericException("Product ${line.locationProductId} has no unit price")
        PricedSaleLine(line.locationProductId, line.quantity, unitPrice)
    }

    private fun applyTotals(sale: SaleEntity, lines: List<SaleLineEntity>, discounts: List<SaleDiscountSummaryDto>) {
        sale.subtotal = lines.sumOf { it.lineTotal() }
        sale.discountTotal = discounts.sumOf { it.calculatedAmount }
    }

    fun update(
        dto: SaleUpdateDto,
        sale: SaleEntity,
        postLines: (List<SaleLineEntity>) -> Unit = {},
    ): SaleUpdateOutcome {
        removeLines(dto.linesToRemove)
        val updateResult = saleLinesUpdater.applyLineUpdates(sale.id!!, dto)
        postLines(updateResult.survivingSaleLines)
        saleDiscountService.removeDiscounts(sale, dto.discountsToRemove)
        val discounts = saleDiscountService.addDiscounts(sale, dto.discountsToAdd, updateResult.survivingSaleLines)
        applyTotals(sale, updateResult.survivingSaleLines, discounts)
        recordPayments(dto.payments, sale)
        return SaleUpdateOutcome(updateResult, discounts)
    }

    private fun removeLines(lineIds: List<UUID>) {
        if (lineIds.isEmpty()) return
        saleDiscountService.removeDiscountsByLineIds(lineIds)
        saleLineRepository.deleteAllById(lineIds)
    }

    private fun recordPayments(payments: List<SalePaymentCreateDto>, sale: SaleEntity) {
        if (payments.isEmpty()) return
        salePaymentService.recordPaymentsSubmittedWithSale(sale.id!!, sale.contactId, payments, sale.subtotal!!)
    }
    
}
