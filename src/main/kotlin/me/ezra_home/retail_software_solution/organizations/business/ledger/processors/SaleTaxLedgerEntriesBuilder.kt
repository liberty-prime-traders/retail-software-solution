package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxEntryDto
import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxEntryService
import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxSourceType
import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.organizations.business.org_jurisdiction_tax_type.api.OrgJurisdictionTaxTypeFetcher
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class SaleTaxLedgerEntriesBuilder(
    private val taxEntryService: TaxEntryService,
    private val orgJurisdictionTaxTypeFetcher: OrgJurisdictionTaxTypeFetcher
) {

    fun buildTransactionLevelEntries(saleReferenceNumber: String): List<LedgerEntryRequest> {
        val entries = taxEntryService.findBySourceReference(saleReferenceNumber, TaxSourceType.SALE)
        val payableAccountCodesByTaxTypeId = payableAccountCodesByTaxTypeId()
        return entries.flatMap { entry -> journalFor(entry, payableAccountCodesByTaxTypeId, reversal = false) }
    }

    fun buildTransactionLevelReversalEntries(saleReferenceNumber: String): List<LedgerEntryRequest> {
        val entries = taxEntryService.findBySourceReference(saleReferenceNumber, TaxSourceType.SALE_VOID)
        val payableAccountCodesByTaxTypeId = payableAccountCodesByTaxTypeId()
        return entries.flatMap { entry -> journalFor(entry, payableAccountCodesByTaxTypeId, reversal = true) }
    }

    private fun journalFor(
        taxEntryDto: TaxEntryDto,
        payableAccountCodesByTaxTypeId: Map<UUID, String>,
        reversal: Boolean
    ): List<LedgerEntryRequest> {
        val taxAmount = taxEntryDto.taxAmount.abs()
        val payableAccountCode = payableAccountCodesByTaxTypeId[taxEntryDto.taxTypeId]
            ?: throwMissingPayableAccountCodeException(taxEntryDto)

        return when {
            taxEntryDto.taxIsBilledToCustomerSeparately ->
                listOf(payableLeg(payableAccountCode, taxAmount, creditingPayable = !reversal))

            taxEntryDto.taxIsIncludedInTaxableAmount -> listOf(
                grossSalesLeg(taxAmount, debitingGrossSales = !reversal),
                payableLeg(payableAccountCode, taxAmount, creditingPayable = !reversal)
            )

            else -> listOf(
                taxExpenseLeg(taxAmount, debitingExpense = !reversal),
                payableLeg(payableAccountCode, taxAmount, creditingPayable = !reversal)
            )
        }
    }

    private fun payableLeg(payableAccountCode: String, taxAmount: BigDecimal, creditingPayable: Boolean) =
        LedgerEntryRequest(payableAccountCode, if (creditingPayable) EntryType.CREDIT else EntryType.DEBIT, taxAmount)

    private fun grossSalesLeg(taxAmount: BigDecimal, debitingGrossSales: Boolean) =
        LedgerEntryRequest(SystemAccount.GROSS_SALES.code, if (debitingGrossSales) EntryType.DEBIT else EntryType.CREDIT, taxAmount)

    private fun taxExpenseLeg(taxAmount: BigDecimal, debitingExpense: Boolean) =
        LedgerEntryRequest(SystemAccount.TAX_EXPENSE.code, if (debitingExpense) EntryType.DEBIT else EntryType.CREDIT, taxAmount)

    private fun payableAccountCodesByTaxTypeId(): Map<UUID, String> =
        orgJurisdictionTaxTypeFetcher.getAllDtos().associate { it.jurisdictionTaxTypeId to it.payableAccountCode }

    private fun throwMissingPayableAccountCodeException(taxEntryDto: TaxEntryDto): Nothing {
        throw RtsGenericException(
            "Missing payable account code for tax type ${taxEntryDto.taxTypeName} in jurisdiction ${taxEntryDto.jurisdictionName}. " +
                    "Please configure the payable account code for this tax type in the organization settings."
        )
    }
}
