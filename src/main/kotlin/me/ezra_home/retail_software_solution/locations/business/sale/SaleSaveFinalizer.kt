package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleSaveRequest
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.SaleAdjustmentSyncer
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.SaleAdjustmentFetcher
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.PersistedSalePayment
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentAppender
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SaleSaveFinalizer(
    private val saleRepository: SaleRepository,
    private val saleAdjustmentSyncer: SaleAdjustmentSyncer,
    private val saleAdjustmentFetcher: SaleAdjustmentFetcher,
    private val saleTotalsApplier: SaleTotalsApplier,
    private val salePaymentAppender: SalePaymentAppender,
) {

    fun finalize(
        saleEntity: SaleEntity,
        saleSaveRequest: SaleSaveRequest,
        lineSyncResult: SaleLineSync.Result,
    ): SaleSaveFinalizeResult {
        val saleId = saleEntity.id!!
        val saleAdjustmentIdsByClientKey = saleAdjustmentSyncer.sync(
            saleId = saleId,
            saleSaveRequest = saleSaveRequest,
            persistedSaleLines = lineSyncResult.toPersistedSaleLines(),
            saleLineIdsByClientKey = lineSyncResult.saleLineIdsByClientKey,
        )
        saleTotalsApplier.applyTotals(
            saleEntity,
            lineSyncResult.persistedSaleLines,
            saleAdjustmentFetcher.getAdjustmentSummaries(saleId),
        )
        val salePaymentAppendResult = salePaymentAppender.appendNew(
            saleId = saleId,
            contactId = saleEntity.contactId,
            payableTotal = saleEntity.payableTotal(),
            saleSaveRequest = saleSaveRequest,
        )
        saleEntity.paymentStatus = salePaymentAppendResult.newPaymentStatus
        saleRepository.save(saleEntity)
        return SaleSaveFinalizeResult(
            saleAdjustmentIdsByClientKey = saleAdjustmentIdsByClientKey,
            persistedSalePaymentsByClientKey = salePaymentAppendResult.persistedSalePaymentsByClientKey,
        )
    }

    data class SaleSaveFinalizeResult(
        val saleAdjustmentIdsByClientKey: Map<UUID, UUID>,
        val persistedSalePaymentsByClientKey: Map<UUID, PersistedSalePayment>,
    )
}
