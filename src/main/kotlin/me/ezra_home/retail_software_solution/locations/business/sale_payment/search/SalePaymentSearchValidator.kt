package me.ezra_home.retail_software_solution.locations.business.sale_payment.search

import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentSearchParameters
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException

object SalePaymentSearchValidator {

  private const val MAX_SALE_REFERENCE_NUMBERS = 20
  const val MIN_PAGE_SIZE = 1
  const val MAX_PAGE_SIZE = 1000

  fun guardValidParameters(salePaymentSearchParameters: SalePaymentSearchParameters) {
    if (salePaymentSearchParameters.saleReferenceNumbers.size > MAX_SALE_REFERENCE_NUMBERS) {
      throw RtsGenericException("A maximum of $MAX_SALE_REFERENCE_NUMBERS sale reference numbers may be supplied")
    }
    if (salePaymentSearchParameters.minAmount != null && salePaymentSearchParameters.maxAmount != null && salePaymentSearchParameters.minAmount > salePaymentSearchParameters.maxAmount) {
      throw RtsGenericException("minAmount must not be greater than maxAmount")
    }
    if (salePaymentSearchParameters.recordedFrom != null && salePaymentSearchParameters.recordedBefore != null && salePaymentSearchParameters.recordedFrom.isAfter(salePaymentSearchParameters.recordedBefore)) {
      throw RtsGenericException("recordedFrom must not be after recordedBefore")
    }
    if (salePaymentSearchParameters.paymentDateFrom != null && salePaymentSearchParameters.paymentDateBefore != null && salePaymentSearchParameters.paymentDateFrom.isAfter(salePaymentSearchParameters.paymentDateBefore)) {
      throw RtsGenericException("paymentDateFrom must not be after paymentDateBefore")
    }
  }

  fun guardValidPageSize(requestedSize: Int) {
    if (requestedSize !in MIN_PAGE_SIZE..MAX_PAGE_SIZE) {
      throw RtsGenericException("requestedSize must be between $MIN_PAGE_SIZE and $MAX_PAGE_SIZE")
    }
  }

  fun guardSummaryHasFilter(salePaymentSearchParameters: SalePaymentSearchParameters) {
    val hasFilter = salePaymentSearchParameters.recordedFrom != null ||
      salePaymentSearchParameters.recordedBefore != null ||
      salePaymentSearchParameters.paymentDateFrom != null ||
      salePaymentSearchParameters.paymentDateBefore != null ||
      salePaymentSearchParameters.contactIds.isNotEmpty() ||
      salePaymentSearchParameters.paymentMethodIds.isNotEmpty() ||
      salePaymentSearchParameters.statuses.isNotEmpty() ||
      salePaymentSearchParameters.minAmount != null ||
      salePaymentSearchParameters.maxAmount != null ||
      salePaymentSearchParameters.saleReferenceNumbers.isNotEmpty()
    if (!hasFilter) {
      throw RtsGenericException("At least one filter must be supplied to summarize sale payments")
    }
  }
}
