package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleCommitInput
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.SaleAdjustmentCommitter
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.SaleAdjustmentFetcher
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentCommitter
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SaleCommitFinalizer(
    private val saleRepository: SaleRepository,
    private val saleAdjustmentCommitter: SaleAdjustmentCommitter,
    private val saleAdjustmentFetcher: SaleAdjustmentFetcher,
    private val saleTotalsApplier: SaleTotalsApplier,
    private val salePaymentCommitter: SalePaymentCommitter,
) {

    fun finalize(
        sale: SaleEntity,
        input: SaleCommitInput,
        lineSyncResult: SaleCommitLineSync.Result,
    ): SaleUpdateResult {
        val saleId = sale.id!!
        val adjustmentIdByClientKey = saleAdjustmentCommitter.sync(
            saleId = saleId,
            input = input,
            persistedLines = lineSyncResult.toPersistedCommitLines(),
            saleLineIdByClientKey = lineSyncResult.saleLineIdByClientKey,
        )
        saleTotalsApplier.applyTotals(
            sale,
            lineSyncResult.persistedLines,
            saleAdjustmentFetcher.getAdjustmentSummaries(saleId),
        )
        val paymentResult = salePaymentCommitter.appendNew(
            saleId = saleId, contactId = sale.contactId,
            payableTotal = sale.payableTotal(), input = input,
        )
        sale.paymentStatus = paymentResult.newStatus
        saleRepository.save(sale)
        return SaleUpdateResult(adjustmentIdByClientKey, paymentResult.idsByClientKey)
    }

    data class SaleUpdateResult(
        val adjustmentIdsByClientKey: Map<UUID, UUID>,
        val paymentIdsByClientKey: Map<UUID, UUID>,
    )
}
