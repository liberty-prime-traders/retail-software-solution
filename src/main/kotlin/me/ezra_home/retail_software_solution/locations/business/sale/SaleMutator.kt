package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductLineWithPrice
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleCreateDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineCreateDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleUpdateDto
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.NewSaleAdjustmentValidator
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.SaleAdjustmentReconciler
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.SaleAdjustmentService
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.SaleAdjustmentSummaryDto
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.SaleAdjustmentValidator
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentCreateDto
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentService
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.util.enums.SystemContact
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID


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
    private val saleAdjustmentService: SaleAdjustmentService,
    private val saleAdjustmentValidator: SaleAdjustmentValidator,
    private val newSaleAdjustmentValidator: NewSaleAdjustmentValidator,
    private val saleAdjustmentReconciler: SaleAdjustmentReconciler
) {

    fun create(dto: SaleCreateDto, sale: SaleEntity, insertContext: SaleLinesInsertContext): SaleCreateOutcome {
        val pricedLines = attachUnitPrices(dto.linesToAdd, insertContext.productSummaries)
        newSaleAdjustmentValidator.validateNewAdjustments(dto.adjustments, pricedLines, insertContext.productSummaries)
        val enforceTotals = sale.status != SaleStatus.DRAFT
        if (enforceTotals) {
            newSaleAdjustmentValidator.guardAdjustmentCeilings(dto.adjustments, pricedLines, insertContext.productSummaries)
        }
        saleRepository.save(sale)
        val saleLineEntities = SaleLineMapper.toLineEntities(sale.id!!, dto.linesToAdd, insertContext)
        saleLineRepository.saveAll(saleLineEntities)
        if (sale.status == SaleStatus.DRAFT) {
            saleStockReserver.reserve(sale.id!!, saleLineEntities)
        }
        val adjustments = saleAdjustmentService.applyValidatedAdjustments(sale.id!!, dto.adjustments, saleLineEntities)
        applyTotals(sale, saleLineEntities, adjustments)
        if (enforceTotals) {
            val payableTotal = sale.payableTotal()
            salePaymentService.guardPaymentsWithinSaleTotal(sale.id!!, dto.payments, payableTotal, isNewSale = true)
            if (sale.contactId == SystemContact.WALK_IN.id) {
                salePaymentService.guardFullPaymentCoverage(sale.id!!, dto.payments, payableTotal, isNewSale = true)
            }
        }
        recordPayments(dto.payments, sale, true)
        return SaleCreateOutcome(saleLineEntities, insertContext, adjustments)
    }

    private fun attachUnitPrices(
        dtoLines: List<SaleLineCreateDto>,
        productSummaries: Map<UUID, LocationProductSummaryDto>,
    ): List<ProductLineWithPrice> = dtoLines.map { line ->
        val unitPrice = productSummaries.getValue(line.locationProductId).unitPrice
            ?: throw RtsGenericException("Product ${line.locationProductId} has no unit price")
        PricedSaleLine(line.locationProductId, line.quantity, unitPrice)
    }

    private fun applyTotals(
        sale: SaleEntity,
        lines: List<SaleLineEntity>,
        adjustments: List<SaleAdjustmentSummaryDto>,
    ) {
        sale.subtotal = lines.sumOf { it.lineTotal() }
        sale.lineLevelDiscountTotal = adjustments
            .filter { it.direction == AdjustmentDirection.DISCOUNT && it.saleLineId != null }
            .sumOf { it.calculatedAmount }
        sale.orderLevelDiscountTotal = adjustments
            .filter { it.direction == AdjustmentDirection.DISCOUNT && it.saleLineId == null }
            .sumOf { it.calculatedAmount }
        sale.lineLevelSurchargeTotal = adjustments
            .filter { it.direction == AdjustmentDirection.SURCHARGE && it.saleLineId != null }
            .sumOf { it.calculatedAmount }
        sale.orderLevelSurchargeTotal = adjustments
            .filter { it.direction == AdjustmentDirection.SURCHARGE && it.saleLineId == null }
            .sumOf { it.calculatedAmount }
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
        saleAdjustmentValidator.guardAdjustmentsBelongToSale(saleId, dto.adjustmentsToRemove)

        removeLines(sale.status, dto.linesToRemove)
        val updateContext = saleLinesUpdatePreparer.prepareForUpdate(saleId, dto, existingLines)
        saleLinesUpdateApplier.apply(saleId, updateContext)
        if (syncReservations) {
            saleStockReserver.clearByLineIds(dto.linesToRemove)
            saleStockReserver.syncUpdatedReservations(updateContext.updatedLines, updateContext.newLines, saleId)
        }
        val survivingLines = updateContext.survivingLines()
        saleAdjustmentService.removeAdjustments(sale.status, dto.adjustmentsToRemove)
        val linesChanged = dto.linesToAdd.isNotEmpty() || dto.linesToUpdate.isNotEmpty() || dto.linesToRemove.isNotEmpty()
        val reconciled = if (linesChanged) {
            saleAdjustmentReconciler.reconcileAdjustmentsAfterLineChanges(saleId, survivingLines)
        } else {
            saleAdjustmentService.findBySaleId(saleId)
        }
        if (enforceTotals) {
            saleAdjustmentValidator.assertAdjustmentsStillFitAfterLineChanges(reconciled, survivingLines, updateContext.productSummaries)
        }
        val adjustments = saleAdjustmentService.addAdjustments(
            saleId, sale.status, reconciled, dto.adjustmentsToAdd, survivingLines, updateContext.productSummaries,
        )
        if (enforceTotals) {
            val productByLineId = survivingLines.filter { it.id != null }.associate { it.id!! to it.locationProductId }
            newSaleAdjustmentValidator.guardAdjustmentCeilings(
                dto.adjustmentsToAdd, survivingLines, updateContext.productSummaries, reconciled, productByLineId,
            )
        }
        applyTotals(sale, survivingLines, adjustments)
        if (enforceTotals) {
            salePaymentService.guardPaymentsWithinSaleTotal(
                saleId, dto.payments, sale.payableTotal(), isNewSale = false,
            )
        }
        recordPayments(dto.payments, sale, false)
        return SaleUpdateOutcome(survivingLines, updateContext.productSummaries, adjustments)
    }

    private fun removeLines(saleStatus: SaleStatus, lineIds: List<UUID>) {
        if (lineIds.isEmpty()) return
        saleAdjustmentService.removeAdjustmentsByLineIds(saleStatus, lineIds)
        saleLineRepository.deleteAllById(lineIds)
    }

    private fun recordPayments(payments: List<SalePaymentCreateDto>, sale: SaleEntity, isNewSale: Boolean) {
        val newStatus = salePaymentService.recordPaymentsSubmittedWithSale(
            sale.id!!, sale.contactId, payments, sale.payableTotal(), isNewSale,
        )
        if (newStatus != null) sale.paymentStatus = newStatus
    }
}
