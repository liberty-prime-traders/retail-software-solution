package me.ezra_home.retail_software_solution.locations.business.stock.revision

import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementEntity
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAdjustmentMovementDetails
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityAsEntered
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class StockEntryConsumption(
    val stockEntryId: UUID,
    val locationProductId: UUID,
    val consumer: StockConsumer,
    val quantityAsEntered: QuantityAsEntered,
    val orderOfConsumption: OrderOfConsumption,
    val netConsumedStorageQuantity: ExactQuantity,
    val netConsumedCost: BigDecimal
) {
    val stockMovementOrigin: StockMovementOrigin = consumer.stockMovementOrigin

    companion object {
        fun requireOnOneLayer(stockEntryConsumptions: List<StockEntryConsumption>) =
            require(stockEntryConsumptions.map { it.stockEntryId }.distinct().size == 1) {
                "A stock revision replaces one layer, so its consumptions must all sit on one layer."
            }
    }
}

data class StockConsumer(
    val stockMovementOrigin: StockMovementOrigin,
    val movementType: MovementType,
    val adjustmentDetails: StockAdjustmentMovementDetails?,
) {
    companion object {
        fun of(movement: StockMovementEntity) = StockConsumer(
            stockMovementOrigin = StockMovementOrigin.of(movement),
            movementType = movement.movementType,
            adjustmentDetails = movement.category?.let { StockAdjustmentMovementDetails(it, movement.reasonId, movement.note) },
        )
    }
}

data class StockMovementOrigin(
    val documentReference: String?,
    val detailReference: String?,
) {
    val displayText: String = listOfNotNull(documentReference, detailReference).joinToString("/")

    companion object {
        fun of(movement: StockMovementEntity) = StockMovementOrigin(movement.documentReference, movement.detailReference)
    }
}

data class OrderOfConsumption(val createdOn: OffsetDateTime?, val id: UUID?) : Comparable<OrderOfConsumption> {

    override fun compareTo(other: OrderOfConsumption): Int = comparator.compare(this, other)

    companion object {
        private val comparator: Comparator<OrderOfConsumption> =
            compareBy(nullsLast()) { orderOfConsumption: OrderOfConsumption -> orderOfConsumption.createdOn }
                .thenBy(nullsLast()) { orderOfConsumption: OrderOfConsumption -> orderOfConsumption.id }

        fun of(movement: StockMovementEntity) = OrderOfConsumption(movement.createdOn, movement.id)
    }
}
