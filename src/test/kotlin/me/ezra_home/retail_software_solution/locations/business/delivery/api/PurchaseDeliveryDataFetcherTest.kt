package me.ezra_home.retail_software_solution.locations.business.delivery.api

import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryAssembler
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryEntity
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryLineEntity
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryLineRepository
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryRepository
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDeliveryLineStatus
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class PurchaseDeliveryDataFetcherTest {

    private val purchaseLineId = UUID.randomUUID()
    private val purchaseDeliveryLineRepository = mock(PurchaseDeliveryLineRepository::class.java)
    private val purchaseDeliveryRepository = mock(PurchaseDeliveryRepository::class.java)
    private val purchaseDeliveryDataFetcher = PurchaseDeliveryDataFetcher(
        purchaseDeliveryRepository,
        purchaseDeliveryLineRepository,
        mock(PurchaseDeliveryAssembler::class.java)
    )

    private fun deliveryLine(userProvidedQuantity: String, storageUnitsPerUserProvidedUnit: Long) =
        PurchaseDeliveryLineEntity(
            purchaseDeliveryId = UUID.randomUUID(),
            purchaseLineId = purchaseLineId,
            userProvidedQuantityDelivered = BigDecimal(userProvidedQuantity),
            unitCost = BigDecimal("10.00"),
            unitId = UUID.randomUUID(),
            storageUnitsPerUserProvidedUnit = storageUnitsPerUserProvidedUnit,
        )

    @Test
    fun `the delivered quantity sums the active lines in storage units`() {
        `when`(
            purchaseDeliveryLineRepository.findByPurchaseLineIdAndStatus(
                purchaseLineId, PurchaseDeliveryLineStatus.ACTIVE
            )
        ).thenReturn(listOf(deliveryLine("2", 12L), deliveryLine("5", 1L)))

        val delivered = purchaseDeliveryDataFetcher.calculateStorageQuantityDelivered(purchaseLineId)

        assertEquals(ExactQuantity.ofStorage(BigDecimal("29")), delivered)
    }

    @Test
    fun `a delivery total counts the active lines only`() {
        val deliveryId = UUID.randomUUID()
        `when`(
            purchaseDeliveryLineRepository.findByPurchaseDeliveryIdInAndStatus(
                listOf(deliveryId), PurchaseDeliveryLineStatus.ACTIVE
            )
        ).thenReturn(listOf(deliveryLine("2", 1L), deliveryLine("3", 1L)))

        assertEquals(
            0, BigDecimal("50.00").compareTo(purchaseDeliveryDataFetcher.calculateSingleDeliveryTotal(deliveryId))
        )
    }

    @Test
    fun `a purchase total counts the active lines only`() {
        val purchaseId = UUID.randomUUID()
        val deliveryId = UUID.randomUUID()
        `when`(purchaseDeliveryRepository.findByPurchaseIdIn(listOf(purchaseId))).thenReturn(
            listOf(PurchaseDeliveryEntity(purchaseId, deliveredAt = OffsetDateTime.now()).also { it.id = deliveryId })
        )
        `when`(
            purchaseDeliveryLineRepository.findByPurchaseDeliveryIdInAndStatus(
                listOf(deliveryId), PurchaseDeliveryLineStatus.ACTIVE
            )
        ).thenReturn(listOf(deliveryLine("2", 1L)))

        assertEquals(0, BigDecimal("20.00").compareTo(purchaseDeliveryDataFetcher.calculateDeliveredTotal(purchaseId)))
    }

    @Test
    fun `a line with no active deliveries has delivered nothing`() {
        `when`(
            purchaseDeliveryLineRepository.findByPurchaseLineIdAndStatus(
                purchaseLineId, PurchaseDeliveryLineStatus.ACTIVE
            )
        ).thenReturn(emptyList())

        assertEquals(ExactQuantity.ZERO, purchaseDeliveryDataFetcher.calculateStorageQuantityDelivered(purchaseLineId))
    }
}
