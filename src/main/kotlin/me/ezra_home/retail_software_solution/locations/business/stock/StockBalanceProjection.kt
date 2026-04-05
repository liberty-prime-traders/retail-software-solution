package me.ezra_home.retail_software_solution.locations.business.stock

import java.math.BigDecimal
import java.util.UUID

interface StockBalanceProjection {
  fun getLocationProductId(): UUID
  fun getRemainingQuantity(): BigDecimal
}
