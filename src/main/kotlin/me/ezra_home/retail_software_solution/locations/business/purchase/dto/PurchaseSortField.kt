package me.ezra_home.retail_software_solution.locations.business.purchase.dto

enum class PurchaseSortField(val entityField: String, val columnName: String) {
  DATE_ORDERED("dateOrdered", "date_ordered"),
  CREATED_ON("createdOn", "created_on")
}
