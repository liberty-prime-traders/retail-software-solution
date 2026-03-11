package me.ezra_home.retail_software_solution.util.enums

enum class StockMovementReason(override val code: String) : HasCode {
  COUNT_CORRECTION("CCR"),
  DAMAGE("DM"),
  EXPIRY("EX"),
  PROMOTIONAL_GIVEAWAY("GA"),
  THEFT("TF")
}
