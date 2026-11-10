package me.ezra_home.retail_software_solution.locations.business.delivery.api

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.delivery.PurchaseDeliveryLineStatus
import me.ezra_home.retail_software_solution.locations.business.purchase.api.delivery.PurchaseDeliveryStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class PurchaseDeliveryResponseDtoTest {

    private val unitId = UUID.randomUUID()

    private fun line(quantity: String, status: PurchaseDeliveryLineStatus) = PurchaseDeliveryLineResponseDto(
        id = UUID.randomUUID(),
        referenceNumber = "DELN000001",
        purchaseLineId = UUID.randomUUID(),
        locationProduct = LocationProductSummaryDto(
            UUID.randomUUID(), "LP1", UUID.randomUUID(), "Sugar", "Pantry", unitId
        ),
        userProvidedQuantityDelivered = BigDecimal(quantity),
        unitId = unitId,
        unitCost = BigDecimal("10.00"),
        status = status,
    )

    @Test
    fun `the delivery total ignores revised lines`() {
        val deliveryResponse = PurchaseDeliveryResponseDto(
            id = UUID.randomUUID(),
            referenceNumber = "DEL000001",
            purchaseId = UUID.randomUUID(),
            status = PurchaseDeliveryStatus.RECEIVED,
            deliveredAt = OffsetDateTime.now(),
            notes = null,
            lines = listOf(
                line("10", PurchaseDeliveryLineStatus.REVISED),
                line("8", PurchaseDeliveryLineStatus.ACTIVE),
            ),
        )

        assertEquals(0, BigDecimal("80.00").compareTo(deliveryResponse.deliveryTotal))
    }
}
