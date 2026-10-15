package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.business.ConversionRatio
import java.math.BigDecimal
import java.util.UUID

data class FifoConsumptionRequest(
    val locationProductId: UUID,
    val baseQuantity: BigDecimal,
    val unitId: UUID,
    val conversionRatio: ConversionRatio,
    val movementType: MovementType,
    val externalReferenceNumber: String
)
