package me.ezra_home.retail_software_solution.locations.business.purchase.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductUnitRequestDto
import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductStockBreakdownProvider
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseAssembler
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseLineEntity
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseLineRepository
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseRepository
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseStatusResolver
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseValidator
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.api.PurchasePaymentStatusService
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class PurchaseCanceller(
    private val purchaseRepository: PurchaseRepository,
    private val purchaseLineRepository: PurchaseLineRepository,
    private val purchaseAssembler: PurchaseAssembler,
    private val purchasePaymentStatusService: PurchasePaymentStatusService,
    private val purchaseDataFetcher: PurchaseDataFetcher,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val productStockBreakdownProvider: ProductStockBreakdownProvider,
) {

    fun cancel(purchaseId: UUID, lines: List<PurchaseCancelLinesDto>): PurchaseResponseDto {
        val purchase = purchaseDataFetcher.lockAndGetPurchase(purchaseId)
        PurchaseValidator.guardCanCancelLines(purchase)
        val existingLines = purchaseLineRepository.findByPurchaseId(purchaseId)
        cancelLines(existingLines, lines)
        purchase.purchaseStatus = PurchaseStatusResolver.resolveAfterQuantityChange(existingLines) {
            purchase.purchaseStatus
        }
        purchaseRepository.save(purchase)
        val purchaseResponse = purchaseAssembler.buildResponse(purchase, existingLines)
        return purchaseResponse.copy(
            paymentStatus = purchasePaymentStatusService.patchThenReturnPaymentStatus(
                purchaseId,
                purchaseResponse.paymentCeiling
            )
        )
    }

    private fun cancelLines(existingLines: List<PurchaseLineEntity>, cancels: List<PurchaseCancelLinesDto>) {
        val linesById = existingLines.associateBy { it.id!! }
        val cancelledLines = cancels.mapNotNull { cancel -> linesById[cancel.purchaseLineId]?.let { it to cancel } }
        val storageUnitsPerUserProvidedUnitByLocationProductId = locationProductDataFetcher.getStorageUnitsPerUserProvidedUnit(
            cancelledLines
                .filter { (line, cancel) -> cancel.unitId != null && cancel.unitId != line.unitId }
                .map { (line, cancel) -> LocationProductUnitRequestDto(line.locationProductId, cancel.unitId!!) }
        )
        val breakdownByLocationProductId = productStockBreakdownProvider.forLocationProducts(
            cancelledLines.map { (line, _) -> line.locationProductId }.toSet()
        )
        val toSave = cancelledLines.map { (line, cancel) ->
            val storageUnitsPerUserProvidedUnit = if (cancel.unitId == null || cancel.unitId == line.unitId) {
                line.storageUnitsPerUserProvidedUnit
            } else {
                storageUnitsPerUserProvidedUnitByLocationProductId.getValue(line.locationProductId)
            }
            val storageQuantityToCancel = ExactQuantity.ofStorageFromUserProvided(cancel.userProvidedQuantityCanceled, storageUnitsPerUserProvidedUnit)
            PurchaseValidator.guardCancelQuantity(line, storageQuantityToCancel, breakdownByLocationProductId.getValue(line.locationProductId))
            line.storageQuantityCanceled = storageQuantityToCancel.toStorageQuantity()
            line
        }
        purchaseLineRepository.saveAll(toSave)
    }
}
