package me.ezra_home.retail_software_solution.locations.business.stock_revision.api

import me.ezra_home.retail_software_solution.util.ui_models.BalanceEffect
import java.math.BigDecimal

// Declaration order is the display order: blocking items first.
enum class RevisionPreviewSeverity {
    BLOCKS_SAVE,
    WARNING,
    NOTE,
}

data class RevisionPreviewItemDto(
    val severity: RevisionPreviewSeverity,
    val documentReference: String,
    val description: String,
    val effect: String,
    val netProfitChange: BigDecimal? = null,
)

// accountText names the account and what the line does to its own balance, such as "Trade Payables (decreases)".
// balanceEffect is what that does to the business: a liability that decreases adds, an asset that decreases subtracts.
data class RevisionPreviewLedgerLineDto(
    val accountText: String,
    val debit: BigDecimal?,
    val credit: BigDecimal?,
    val balanceEffect: BalanceEffect,
)

data class RevisionPreviewLedgerEntryDto(
    val source: String,
    val description: String,
    val lines: List<RevisionPreviewLedgerLineDto>,
)

data class TextSegmentDto(
    val text: String,
    val emphasis: Boolean = false,
)

data class GuidanceParagraphDto(
    val segments: List<TextSegmentDto>,
)

data class DeliveryLineRevisionPreviewDto(
    val revisedDeliveryLineReferenceNumber: String,
    val changeSummary: String,
    val transactions: List<RevisionPreviewItemDto>,
    val guidance: List<GuidanceParagraphDto>,
    val ledgerEntries: List<RevisionPreviewLedgerEntryDto>,
) {
    val blocksSaveCount: Int = transactions.count { it.severity == RevisionPreviewSeverity.BLOCKS_SAVE }
    val warningCount: Int = transactions.count { it.severity == RevisionPreviewSeverity.WARNING }
    val blocksSave: Boolean = blocksSaveCount > 0
}
