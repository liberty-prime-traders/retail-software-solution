package me.ezra_home.retail_software_solution.locations.business.purchase.search

object PurchaseSearchParams {

  object Columns {
    const val SUPPLIER_ID = "supplier_id"
    const val ORDERED_BY = "ordered_by"
    const val CREATED_BY_ID = "created_by_id"
    const val STATUS = "status"
    const val DATE_ORDERED = "date_ordered"
    const val CREATED_ON = "created_on"
  }

  object Params {
    const val SUPPLIER_IDS = "supplierIds"
    const val ORDERED_BY_IDS = "orderedByIds"
    const val CREATED_BY_IDS = "createdByIds"
    const val STATUSES = "statuses"
    const val DATE_ORDERED_FROM = "dateOrderedFrom"
    const val DATE_ORDERED_TO = "dateOrderedTo"
    const val CREATED_ON_FROM = "createdOnFrom"
    const val CREATED_ON_TO = "createdOnTo"
    const val LIMIT = "limit"
  }
}
