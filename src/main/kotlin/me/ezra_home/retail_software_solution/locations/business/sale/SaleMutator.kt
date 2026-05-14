package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductLineWithPrice
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleCreateDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineCreateDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleUpdateDto
import me.ezra_home.retail_software_solution.locations.business.sale_discount.api.NewSaleDiscountValidator
import me.ezra_home.retail_software_solution.locations.business.sale_discount.api.SaleDiscountReconciler
import me.ezra_home.retail_software_solution.locations.business.sale_discount.api.SaleDiscountService
import me.ezra_home.retail_software_solution.locations.business.sale_discount.api.SaleDiscountSummaryDto
import me.ezra_home.retail_software_solution.locations.business.sale_discount.api.SaleDiscountValidator
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentCreateDto
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentService
import me.ezra_home.retail_software_solution.util.enums.SystemContact
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

data class SaleCreateOutcome(
    val lines: List<SaleLineEntity>,
    val insertContext: SaleLinesInsertContext,
    val discounts: List<SaleDiscountSummaryDto>,
)

data class SaleUpdateOutcome(
    val survivingLines: List<SaleLineEntity>,
    val productSummaries: Map<UUID, LocationProductSummaryDto>,
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
    private val saleLinesUpdatePreparer: SaleLinesUpdatePreparer,
    private val saleLinesUpdateApplier: SaleLinesUpdateApplier,
    private val salePaymentService: SalePaymentService,
    private val saleStockReserver: SaleStockReserver,
    private val saleDiscountService: SaleDiscountService,
    private val saleDiscountValidator: SaleDiscountValidator,
    private val newSaleDiscountValidator: NewSaleDiscountValidator,
    private val saleDiscountReconciler: SaleDiscountReconciler
) {

    fun create(dto: SaleCreateDto, sale: SaleEntity, insertContext: SaleLinesInsertContext): SaleCreateOutcome {
        val pricedLines = attachUnitPrices(dto.linesToAdd, insertContext.productSummaries)
        newSaleDiscountValidator.validateNewDiscounts(dto.discounts, pricedLines, insertContext.productSummaries)
        val enforceTotals = sale.status != SaleStatus.DRAFT
        if (enforceTotals) {
            newSaleDiscountValidator.guardDiscountTotals(dto.discounts, pricedLines, insertContext.productSummaries)
        }
        saleRepository.save(sale)
        val saleLineEntities = SaleLineMapper.toLineEntities(sale.id!!, dto.linesToAdd, insertContext)
        saleLineRepository.saveAll(saleLineEntities)
        if (sale.status == SaleStatus.DRAFT) {
            saleStockReserver.reserve(sale.id!!, saleLineEntities)
        }
        val discounts = saleDiscountService.applyValidatedDiscounts(sale.id!!, dto.discounts, saleLineEntities)
        applyTotals(sale, saleLineEntities, discounts)
        if (enforceTotals) {
            val payableTotal = sale.payableTotal()
            salePaymentService.guardPaymentsWithinSaleTotal(sale.id!!, dto.payments, payableTotal, isNewSale = true)
            if (sale.contactId == SystemContact.WALK_IN.id) {
                salePaymentService.guardFullPaymentCoverage(sale.id!!, dto.payments, payableTotal, isNewSale = true)
            }
        }
        recordPayments(dto.payments, sale, true)
        return SaleCreateOutcome(saleLineEntities, insertContext, discounts)
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

    fun updateAndSyncReservations(dto: SaleUpdateDto, sale: SaleEntity): SaleUpdateOutcome =
        doUpdate(dto, sale, syncReservations = true, enforceTotals = false)

    fun updateWithoutSyncingReservations(dto: SaleUpdateDto, sale: SaleEntity): SaleUpdateOutcome =
        doUpdate(dto, sale, syncReservations = false, enforceTotals = true)

    private fun doUpdate(
        dto: SaleUpdateDto,
        sale: SaleEntity,
        syncReservations: Boolean,
        enforceTotals: Boolean,
    ): SaleUpdateOutcome {
        val saleId = sale.id!!
        val existingLines = saleLineRepository.findBySaleId(saleId)
        SaleValidator.guardLineIdsBelongToSale(dto, existingLines.mapTo(HashSet()) { it.id!! })
        saleDiscountValidator.guardDiscountsBelongToSale(saleId, dto.discountsToRemove)

        removeLines(sale.status, dto.linesToRemove)
        val updateContext = saleLinesUpdatePreparer.prepareForUpdate(saleId, dto, existingLines)
        saleLinesUpdateApplier.apply(saleId, updateContext)
        if (syncReservations) {
            saleStockReserver.clearByLineIds(dto.linesToRemove)
            saleStockReserver.syncUpdatedReservations(updateContext.updatedLines, updateContext.newLines, saleId)
        }
        val survivingLines = updateContext.survivingLines()
        saleDiscountService.removeDiscounts(sale.status, dto.discountsToRemove)
        val linesChanged = dto.linesToAdd.isNotEmpty() || dto.linesToUpdate.isNotEmpty() || dto.linesToRemove.isNotEmpty()
        val reconciled = if (linesChanged) {
            saleDiscountReconciler.reconcileDiscountsAfterLineChanges(saleId, survivingLines)
        } else {
            saleDiscountService.findBySaleId(saleId)
        }
        if (enforceTotals) {
            saleDiscountValidator.assertDiscountsStillFitAfterLineChanges(reconciled, survivingLines, updateContext.productSummaries)
        }
        val discounts = saleDiscountService.addDiscounts(saleId, sale.status, reconciled, dto.discountsToAdd, survivingLines, updateContext.productSummaries)
        if (enforceTotals) {
            val productByLineId = survivingLines.filter { it.id != null }.associate { it.id!! to it.locationProductId }
            newSaleDiscountValidator.guardDiscountTotals(
                dto.discountsToAdd, survivingLines, updateContext.productSummaries, reconciled, productByLineId,
            )
        }
        applyTotals(sale, survivingLines, discounts)
        if (enforceTotals) {
            salePaymentService.guardPaymentsWithinSaleTotal(
                saleId, dto.payments, sale.payableTotal(), isNewSale = false,
            )
        }
        recordPayments(dto.payments, sale, false)
        return SaleUpdateOutcome(survivingLines, updateContext.productSummaries, discounts)
    }

    private fun removeLines(saleStatus: SaleStatus, lineIds: List<UUID>) {
        if (lineIds.isEmpty()) return
        saleDiscountService.removeDiscountsByLineIds(saleStatus, lineIds)
        saleLineRepository.deleteAllById(lineIds)
    }

    private fun recordPayments(payments: List<SalePaymentCreateDto>, sale: SaleEntity, isNewSale: Boolean) {
        val newStatus = salePaymentService.recordPaymentsSubmittedWithSale(
            sale.id!!, sale.contactId, payments, sale.payableTotal(), isNewSale,
        )
        if (newStatus != null) sale.paymentStatus = newStatus
    }
    
}
