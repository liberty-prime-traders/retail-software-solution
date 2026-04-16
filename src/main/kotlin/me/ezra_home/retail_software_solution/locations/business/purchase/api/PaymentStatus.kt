package me.ezra_home.retail_software_solution.locations.business.purchase.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class PaymentStatus(override val code: String) : HasCode {
  UNPAID("UNP"),
  PARTIALLY_SETTLED("PST"),
  FULLY_SETTLED("FST");
}
