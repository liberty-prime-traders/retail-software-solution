package me.ezra_home.retail_software_solution.locations.business.delivery

import java.math.BigDecimal
import java.util.UUID

interface LastDeliveredCostProjection {
  val locationProductId: UUID
  val unitCost: BigDecimal
  val storageUnitsPerUserProvidedUnit: Long
}
