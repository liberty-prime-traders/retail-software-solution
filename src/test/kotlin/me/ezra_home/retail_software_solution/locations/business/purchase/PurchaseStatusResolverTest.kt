package me.ezra_home.retail_software_solution.locations.business.purchase

import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class PurchaseStatusResolverTest {

    private fun purchaseLine(delivered: String, canceled: String = "0") = PurchaseLineEntity(
        purchaseId = UUID.randomUUID(),
        locationProductId = UUID.randomUUID(),
        userProvidedQuantityOrdered = BigDecimal("18"),
        unitCost = BigDecimal("1.00"),
        unitId = UUID.randomUUID(),
        storageUnitsPerUserProvidedUnit = 1L,
        storageQuantityDelivered = BigDecimal(delivered),
        storageQuantityCanceled = BigDecimal(canceled),
    )

    @Test
    fun `every line fully delivered is fully delivered`() {
        assertEquals(
            PurchaseStatus.FULLY_DELIVERED,
            PurchaseStatusResolver.resolveAfterQuantityChange(listOf(purchaseLine("18")))
        )
    }

    @Test
    fun `some delivery with open lines is partially delivered`() {
        assertEquals(
            PurchaseStatus.PARTIALLY_DELIVERED,
            PurchaseStatusResolver.resolveAfterQuantityChange(listOf(purchaseLine("6")))
        )
    }

    @Test
    fun `everything canceled with nothing delivered is canceled`() {
        assertEquals(
            PurchaseStatus.CANCELED,
            PurchaseStatusResolver.resolveAfterQuantityChange(listOf(purchaseLine("0", canceled = "18")))
        )
    }

    @Test
    fun `nothing delivered with open lines falls back to ordered by default`() {
        assertEquals(
            PurchaseStatus.ORDERED,
            PurchaseStatusResolver.resolveAfterQuantityChange(listOf(purchaseLine("0")))
        )
    }

    @Test
    fun `nothing delivered with open lines uses the caller's fallback`() {
        assertEquals(
            PurchaseStatus.DRAFT,
            PurchaseStatusResolver.resolveAfterQuantityChange(listOf(purchaseLine("0"))) { PurchaseStatus.DRAFT }
        )
    }
}
