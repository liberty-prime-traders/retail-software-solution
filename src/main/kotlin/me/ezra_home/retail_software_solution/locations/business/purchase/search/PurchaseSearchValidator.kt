package me.ezra_home.retail_software_solution.locations.business.purchase.search

import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseSearchParameters
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.queries.SearchGuards

object PurchaseSearchValidator {

  private const val MAX_PURCHASE_REFERENCE_NUMBERS = 20
  private const val MAX_SUPPLIER_IDS = 100
  private const val MIN_PAGE_SIZE = 1
  private const val MAX_PAGE_SIZE = 200

  fun guardValidParameters(purchaseSearchParameters: PurchaseSearchParameters) {
    SearchGuards.guardMaxSize(
      purchaseSearchParameters.purchaseReferenceNumbers.size,
      MAX_PURCHASE_REFERENCE_NUMBERS,
      "purchase reference numbers"
    )

    SearchGuards.guardMaxSize(
      purchaseSearchParameters.supplierIds.size,
      MAX_SUPPLIER_IDS,
      "supplier ids"
    )

    SearchGuards.guardRangeOrder(
      purchaseSearchParameters.minAmount,
      purchaseSearchParameters.maxAmount,
      "minAmount must not be greater than maxAmount"
    ) { min, max -> min > max }

    SearchGuards.guardRangeOrder(
      purchaseSearchParameters.recordedFrom,
      purchaseSearchParameters.recordedBefore,
      "recordedFrom must not be after recordedBefore"
    ) { from, before -> from.isAfter(before) }

    SearchGuards.guardRangeOrder(
      purchaseSearchParameters.purchaseDateFrom,
      purchaseSearchParameters.purchaseDateBefore,
      "purchaseDateFrom must not be after purchaseDateBefore"
    ) { from, before -> from.isAfter(before) }
  }

  fun guardValidPageSize(requestedSize: Int) {
    SearchGuards.guardPageSize(requestedSize, MIN_PAGE_SIZE, MAX_PAGE_SIZE)
  }

  fun guardSummaryHasFilter(purchaseSearchParameters: PurchaseSearchParameters) {
    if (purchaseSearchParameters == PurchaseSearchParameters()) {
      throw RtsGenericException("At least one filter must be supplied to summarize purchases")
    }
  }
}
