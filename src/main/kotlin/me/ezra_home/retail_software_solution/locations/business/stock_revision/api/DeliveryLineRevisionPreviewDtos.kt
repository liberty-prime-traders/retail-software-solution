package me.ezra_home.retail_software_solution.locations.business.stock_revision.api

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

data class RevisionPreviewLedgerLineDto(
    val accountName: String,
    val debit: String?,
    val credit: String?,
)

data class RevisionPreviewLedgerEntryDto(
    val source: String,
    val description: String,
    val lines: List<RevisionPreviewLedgerLineDto>,
)

data class DeliveryLineRevisionPreviewDto(
    val revisedDeliveryLineReferenceNumber: String,
    val changeSummary: String,
    val transactions: List<RevisionPreviewItemDto>,
    val ledgerEntries: List<RevisionPreviewLedgerEntryDto>,
) {
    val blocksSaveCount: Int = transactions.count { it.severity == RevisionPreviewSeverity.BLOCKS_SAVE }
    val warningCount: Int = transactions.count { it.severity == RevisionPreviewSeverity.WARNING }
    val blocksSave: Boolean = blocksSaveCount > 0
}
