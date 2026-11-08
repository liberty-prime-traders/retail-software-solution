package me.ezra_home.retail_software_solution.locations.business.delivery.api

import me.ezra_home.retail_software_solution.util.exceptions.BlockingProblem
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import java.math.BigDecimal
import java.util.UUID

data class ValidatedDeliveryLineRevisionDto(
    val revisedDeliveryLineId: UUID,
    val deliveryReferenceNumber: String,
    val revisedLineReferenceNumber: String,
    val purchaseLineId: UUID,
    val locationProductId: UUID,
    val unitId: UUID,
    val storageUnitsPerUserProvidedUnit: Long,
    val revisedUserProvidedQuantityDelivered: BigDecimal,
    val revisedUnitCost: BigDecimal,
    val userProvidedQuantityDelivered: BigDecimal,
    val unitCost: BigDecimal,
    val problems: List<BlockingProblem>,
) {
    val storageQuantityChange: ExactQuantity =
        ExactQuantity.ofStorageFromUserProvided(userProvidedQuantityDelivered, storageUnitsPerUserProvidedUnit) -
            ExactQuantity.ofStorageFromUserProvided(
                revisedUserProvidedQuantityDelivered, storageUnitsPerUserProvidedUnit
            )
}
