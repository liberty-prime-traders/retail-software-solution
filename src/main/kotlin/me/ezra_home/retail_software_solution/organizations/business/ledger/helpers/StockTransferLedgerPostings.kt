package me.ezra_home.retail_software_solution.organizations.business.ledger.helpers

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryGroupRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderDataFetcher
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component

@Component
class StockTransferLedgerPostings(
    private val stockTransferOrderDataFetcher: StockTransferOrderDataFetcher,
    private val ledgerEntryGroupRepository: LedgerEntryGroupRepository
) {

    // Fails instead of skipping: a skipped event writes no log row, so neither the sweeper nor a retry would revisit it
    // once the dispatch posts, and the in-transit balance would never clear.
    @TransactionalOnOrganizationSchema(readOnly = true)
    fun requireDispatchPosted(orderReferenceNumber: String, dispatchReferenceNumber: String) {
        val sourceLocationId = stockTransferOrderDataFetcher.getByReferenceNumber(orderReferenceNumber).sourceLocationId
        val dispatchPosted = ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceTypeAndSourceLocationId(
            dispatchReferenceNumber, LedgerSourceType.STOCK_TRANSFER_DISPATCH, sourceLocationId
        )
        if (!dispatchPosted) {
            throw RtsGenericException(
                "Cannot clear in transit for $dispatchReferenceNumber: its " +
                    "${LedgerSourceType.STOCK_TRANSFER_DISPATCH.name} posting does not exist"
            )
        }
    }

    fun moveIntoTransit(amount: Money): List<LedgerEntryRequest> =
        LedgerEntryRequest.debitUnlessZero(SystemAccount.INVENTORY_IN_TRANSIT.code, amount.toBigDecimal()) +
            LedgerEntryRequest.creditUnlessZero(SystemAccount.INVENTORY.code, amount.toBigDecimal())

    fun moveOutOfTransit(amount: Money): List<LedgerEntryRequest> =
        LedgerEntryRequest.debitUnlessZero(SystemAccount.INVENTORY.code, amount.toBigDecimal()) +
            LedgerEntryRequest.creditUnlessZero(SystemAccount.INVENTORY_IN_TRANSIT.code, amount.toBigDecimal())
}
