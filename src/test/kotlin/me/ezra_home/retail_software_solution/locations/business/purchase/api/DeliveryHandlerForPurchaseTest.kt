package me.ezra_home.retail_software_solution.locations.business.purchase.api

import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseAssembler
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseEntity
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseLineEntity
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseLineRepository
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseRepository
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

class DeliveryHandlerForPurchaseTest {

    private val purchaseId = UUID.randomUUID()
    private val purchaseLineId = UUID.randomUUID()
    private val purchase = PurchaseEntity(supplierId = UUID.randomUUID(), purchaseStatus = PurchaseStatus.ORDERED)
    private val purchaseLine = PurchaseLineEntity(
        purchaseId = purchaseId,
        locationProductId = UUID.randomUUID(),
        userProvidedQuantityOrdered = BigDecimal("1.0000"),
        unitCost = BigDecimal("10.00"),
        unitId = UUID.randomUUID(),
        storageUnitsPerUserProvidedUnit = 18L,
    )
    private val deliveryHandlerForPurchase = DeliveryHandlerForPurchase(
        purchaseRepository = mock(PurchaseRepository::class.java).also {
            `when`(it.getReferenceById(purchaseId)).thenReturn(purchase)
        },
        purchaseLineRepository = mock(PurchaseLineRepository::class.java).also {
            `when`(it.findByPurchaseId(purchaseId)).thenReturn(listOf(purchaseLine))
        },
        purchaseAssembler = mock(PurchaseAssembler::class.java),
        purchaseDataFetcher = mock(PurchaseDataFetcher::class.java),
    )

    @BeforeEach
    fun assignLineId() {
        purchaseLine.id = purchaseLineId
    }

    private fun deliverStorage(storageQuantity: String) {
        deliveryHandlerForPurchase.commitDelivery(
            purchaseId, listOf(DeliveryLineQuantity(purchaseLineId, ExactQuantity.ofStorage(BigDecimal(storageQuantity))))
        )
    }

    @Test
    fun `delivering 18 single pieces against a 1 bale line completes it exactly`() {
        repeat(17) { deliverStorage("1") }
        assertEquals(PurchaseStatus.PARTIALLY_DELIVERED, purchase.purchaseStatus)

        deliverStorage("1")

        assertEquals(0, BigDecimal("18").compareTo(purchaseLine.storageQuantityDelivered))
        assertEquals(PurchaseStatus.FULLY_DELIVERED, purchase.purchaseStatus)
    }

    @Test
    fun `under-delivery by one storage unit leaves the line open`() {
        deliverStorage("17")

        assertEquals(0, BigDecimal("1").compareTo(purchaseLine.storageQuantityRemaining().toStorageQuantity()))
        assertEquals(PurchaseStatus.PARTIALLY_DELIVERED, purchase.purchaseStatus)
    }

    @Test
    fun `a delivery in a larger unit then single pieces is exact`() {
        deliverStorage("12")
        deliverStorage("6")

        assertEquals(PurchaseStatus.FULLY_DELIVERED, purchase.purchaseStatus)
    }
}
