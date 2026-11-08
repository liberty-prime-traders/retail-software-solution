package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryDataFetcher
import me.ezra_home.retail_software_solution.locations.business.delivery.api.ValidatedDeliveryLineRevisionDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDataFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.revision.ConsumerReplayPlan
import me.ezra_home.retail_software_solution.locations.business.stock_revision.api.RevisionPreviewLedgerEntryDto
import me.ezra_home.retail_software_solution.locations.business.stock_revision.api.RevisionPreviewLedgerLineDto
import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.locations.business.stock_revision.api.AdjustmentCostChange
import me.ezra_home.retail_software_solution.locations.business.stock_revision.api.RevisionLedgerEntries
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.business.DisplayFormatters
import me.ezra_home.retail_software_solution.util.business.mappers.NameResolution
import me.ezra_home.retail_software_solution.util.ui_models.BalanceEffect
import org.springframework.stereotype.Component
import java.math.BigDecimal

// Builds the entries with the same functions the ledger processors post with.
@Component
@TransactionalOnLocationSchema(readOnly = true)
class RevisionLedgerPreviewWriter(
    private val purchaseDeliveryDataFetcher: PurchaseDeliveryDataFetcher,
    private val purchaseDataFetcher: PurchaseDataFetcher,
    private val nameResolution: NameResolution
) {

    fun write(
        validatedDeliveryLineRevision: ValidatedDeliveryLineRevisionDto,
        consumerReplayPlans: List<ConsumerReplayPlan>,
        adjustmentCostChanges: Collection<AdjustmentCostChange>
    ): List<RevisionPreviewLedgerEntryDto> {
        val consumerCostChanges = ConsumerCostChange.ofPlans(consumerReplayPlans)
        return listOfNotNull(
            purchaseDeliveryRevisionEntry(validatedDeliveryLineRevision),
            saleCogsRestatementEntry(consumerCostChanges),
            stockAdjustmentRestatementEntry(consumerCostChanges, adjustmentCostChanges),
        )
    }

    private fun purchaseDeliveryRevisionEntry(
        validatedDeliveryLineRevision: ValidatedDeliveryLineRevisionDto
    ): RevisionPreviewLedgerEntryDto? {
        val revisedLineTotal = Decimals.MoneyScale.multiply(
            validatedDeliveryLineRevision.revisedUserProvidedQuantityDelivered,
            validatedDeliveryLineRevision.revisedUnitCost
        )
        val replacementLineTotal = Decimals.MoneyScale.multiply(
            validatedDeliveryLineRevision.userProvidedQuantityDelivered,
            validatedDeliveryLineRevision.unitCost
        )
        if (revisedLineTotal.compareTo(replacementLineTotal) == 0) return null
        val supplierName = supplierNameOf(validatedDeliveryLineRevision)
        val direction = if (replacementLineTotal > revisedLineTotal) "rises" else "drops"
        return entryOf(
            source = "Purchase delivery revision",
            description = "This delivery line used to be worth ${currency(revisedLineTotal)}. " +
                "It is now worth ${currency(replacementLineTotal)}. " +
                "The amount we owe $supplierName $direction by the same amount.",
            ledgerEntryRequests = RevisionLedgerEntries.purchaseDeliveryRevision(
                revisedLineTotal, replacementLineTotal
            )
        )
    }

    private fun supplierNameOf(validatedDeliveryLineRevision: ValidatedDeliveryLineRevisionDto): String {
        val purchaseId = purchaseDeliveryDataFetcher
            .getOwners(validatedDeliveryLineRevision.revisedDeliveryLineId).purchaseId
        val supplierId = purchaseDataFetcher.findPurchaseInfoByIds(listOf(purchaseId)).getValue(purchaseId).supplierId
        return nameResolution.organizationContacts(listOf(supplierId)).getValue(supplierId)
    }

    private fun saleCogsRestatementEntry(
        consumerCostChanges: List<ConsumerCostChange>
    ): RevisionPreviewLedgerEntryDto? {
        val saleCostChanges = RestatableConsumers.salesOf(consumerCostChanges)
        val oldCostTotal = saleCostChanges.sumOf { it.oldCost }
        val newCostTotal = saleCostChanges.sumOf { it.newCost }
        return entryOf(
            source = "Sale cost restatement",
            description = "Cost of goods sold for the sales that took stock from this delivery goes from " +
                "${currency(oldCostTotal)} to ${currency(newCostTotal)}.",
            ledgerEntryRequests = RevisionLedgerEntries.saleCogsRestatement(oldCostTotal, newCostTotal)
        )
    }

    private fun stockAdjustmentRestatementEntry(
        consumerCostChanges: List<ConsumerCostChange>,
        adjustmentCostChanges: Collection<AdjustmentCostChange>
    ): RevisionPreviewLedgerEntryDto? {
        val adjustmentConsumerCostChanges = RestatableConsumers.stockAdjustmentsOf(consumerCostChanges)
        val oldCostTotal = adjustmentConsumerCostChanges.sumOf { it.oldCost }
        val newCostTotal = adjustmentConsumerCostChanges.sumOf { it.newCost }
        return entryOf(
            source = "Stock adjustment restatement",
            description = "The cost of the stock adjustments that took stock from this delivery goes from " +
                "${currency(oldCostTotal)} to ${currency(newCostTotal)}.",
            ledgerEntryRequests = RevisionLedgerEntries.stockAdjustmentRestatement(adjustmentCostChanges.toList())
        )
    }

    private fun entryOf(
        source: String,
        description: String,
        ledgerEntryRequests: List<LedgerEntryRequest>
    ): RevisionPreviewLedgerEntryDto? {
        if (ledgerEntryRequests.isEmpty()) return null
        return RevisionPreviewLedgerEntryDto(
            source = source,
            description = description,
            lines = ledgerEntryRequests.map { ledgerEntryRequest ->
                val account = accountOf(ledgerEntryRequest.accountCode)
                val amount = ledgerEntryRequest.amount.toBigDecimal()
                val changeToAccount = account.type.amountInNormalDirection(ledgerEntryRequest.entryType, amount)
                val changeText = if (changeToAccount.signum() > 0) "increases" else "decreases"
                RevisionPreviewLedgerLineDto(
                    accountText = "${account.accountName} ($changeText)",
                    debit = amount.takeIf { ledgerEntryRequest.entryType == EntryType.DEBIT },
                    credit = amount.takeIf { ledgerEntryRequest.entryType == EntryType.CREDIT },
                    balanceEffect = BalanceEffect.ofChange(changeToAccount, account.type.balanceEffect()),
                )
            }
        )
    }

    private fun currency(amount: BigDecimal): String = DisplayFormatters.formatCurrency(amount)

    private fun accountOf(accountCode: String): SystemAccount = SystemAccount.entries.first { it.code == accountCode }
}
