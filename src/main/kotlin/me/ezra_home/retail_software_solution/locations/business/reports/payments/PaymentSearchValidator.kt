package me.ezra_home.retail_software_solution.locations.business.reports.payments

import me.ezra_home.retail_software_solution.locations.business.reports.payments.api.PaymentSearchParameters
import me.ezra_home.retail_software_solution.util.queries.SearchGuards

object PaymentSearchValidator {

  private const val MAX_REFERENCE_NUMBERS = 20
  private const val MAX_CONTACT_IDS = 100
  private const val MAX_PAYMENT_METHOD_IDS = 100
  private const val MIN_PAGE_SIZE = 1
  private const val MAX_PAGE_SIZE = 500

  fun guardValidParameters(paymentSearchParameters: PaymentSearchParameters) {
    SearchGuards.guardBoundedRange(
      paymentSearchParameters.recordedFrom,
      paymentSearchParameters.recordedBefore,
      "recordedFrom",
      "recordedBefore"
    )
    paymentSearchParameters.referenceNumbersByType.forEach { (type, referenceNumbers) ->
      SearchGuards.guardMaxSize(referenceNumbers.size, MAX_REFERENCE_NUMBERS, "${type.name.lowercase()} reference numbers")
    }
    SearchGuards.guardMaxSize(paymentSearchParameters.contactIds.size, MAX_CONTACT_IDS, "contact ids")
    SearchGuards.guardMaxSize(paymentSearchParameters.paymentMethodIds.size, MAX_PAYMENT_METHOD_IDS, "payment method ids")

    SearchGuards.guardRangeOrder(
      paymentSearchParameters.minAmount,
      paymentSearchParameters.maxAmount,
      "minAmount must not be greater than maxAmount"
    ) { min, max -> min > max }
    SearchGuards.guardNonNegative(paymentSearchParameters.minAmount, "minAmount")
    SearchGuards.guardNonNegative(paymentSearchParameters.maxAmount, "maxAmount")

    SearchGuards.guardRangeOrder(
      paymentSearchParameters.recordedFrom,
      paymentSearchParameters.recordedBefore,
      "recordedFrom must not be after recordedBefore"
    ) { from, before -> from.isAfter(before) }

    SearchGuards.guardRangeOrder(
      paymentSearchParameters.paymentDateFrom,
      paymentSearchParameters.paymentDateBefore,
      "paymentDateFrom must not be after paymentDateBefore"
    ) { from, before -> from.isAfter(before) }
  }

  fun guardValidPageSize(requestedSize: Int) {
    SearchGuards.guardPageSize(requestedSize, MIN_PAGE_SIZE, MAX_PAGE_SIZE)
  }
}
