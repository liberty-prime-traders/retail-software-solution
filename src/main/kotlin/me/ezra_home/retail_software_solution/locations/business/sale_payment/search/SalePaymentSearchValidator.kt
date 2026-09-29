package me.ezra_home.retail_software_solution.locations.business.sale_payment.search

import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentSearchParameters
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.queries.SearchGuards

object SalePaymentSearchValidator {

  private const val MAX_SALE_REFERENCE_NUMBERS = 20
  private const val MAX_CONTACT_IDS = 100
  private const val MAX_PAYMENT_METHOD_IDS = 100
  private const val MIN_PAGE_SIZE = 1
  private const val MAX_PAGE_SIZE = 500

  fun guardValidParameters(salePaymentSearchParameters: SalePaymentSearchParameters) {
    SearchGuards.guardMaxSize(salePaymentSearchParameters.saleReferenceNumbers.size, MAX_SALE_REFERENCE_NUMBERS, "sale reference numbers")
    SearchGuards.guardMaxSize(salePaymentSearchParameters.contactIds.size, MAX_CONTACT_IDS, "contact ids")
    SearchGuards.guardMaxSize(salePaymentSearchParameters.paymentMethodIds.size, MAX_PAYMENT_METHOD_IDS, "payment method ids")

    SearchGuards.guardRangeOrder(
      salePaymentSearchParameters.minAmount,
      salePaymentSearchParameters.maxAmount,
      "minAmount must not be greater than maxAmount"
    ) { min, max -> min > max }

    SearchGuards.guardRangeOrder(
      salePaymentSearchParameters.recordedFrom,
      salePaymentSearchParameters.recordedBefore,
      "recordedFrom must not be after recordedBefore"
    ) { from, before -> from.isAfter(before) }

    SearchGuards.guardRangeOrder(
      salePaymentSearchParameters.paymentDateFrom,
      salePaymentSearchParameters.paymentDateBefore,
      "paymentDateFrom must not be after paymentDateBefore"
    ) { from, before -> from.isAfter(before) }
  }

  fun guardValidPageSize(requestedSize: Int) {
    SearchGuards.guardPageSize(requestedSize, MIN_PAGE_SIZE, MAX_PAGE_SIZE)
  }

  fun guardSummaryHasFilter(salePaymentSearchParameters: SalePaymentSearchParameters) {
    if (salePaymentSearchParameters == SalePaymentSearchParameters()) {
      throw RtsGenericException("At least one filter must be supplied to summarize sale payments")
    }
  }
}
