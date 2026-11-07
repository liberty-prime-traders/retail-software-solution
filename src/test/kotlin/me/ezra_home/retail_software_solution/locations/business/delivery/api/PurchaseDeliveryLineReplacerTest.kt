package me.ezra_home.retail_software_solution.locations.business.delivery.api

import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryEntity
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryLineEntity
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryLineRepository
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryRepository
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryRevisionValidator
import me.ezra_home.retail_software_solution.locations.business.purchase.api.DeliveryHandlerForPurchase
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDeliveryContext
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDeliveryLineStatus
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineDto
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.UserProvidedQuantity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class PurchaseDeliveryLineReplacerTest {

    private val purchaseDeliveryLineRepository = mock(PurchaseDeliveryLineRepository::class.java)
    private val purchaseDeliveryRepository = mock(PurchaseDeliveryRepository::class.java)
    private val purchaseDeliveryRevisionValidator = mock(PurchaseDeliveryRevisionValidator::class.java)
    private val deliveryHandlerForPurchase = mock(DeliveryHandlerForPurchase::class.java)
    private val purchaseDeliveryLineReplacer = PurchaseDeliveryLineReplacer(
        purchaseDeliveryLineRepository,
        purchaseDeliveryRepository,
        purchaseDeliveryRevisionValidator,
        deliveryHandlerForPurchase
    )

    private val deliveryId = UUID.randomUUID()
    private val revisedLine = PurchaseDeliveryLineEntity(
        purchaseDeliveryId = deliveryId,
        purchaseLineId = UUID.randomUUID(),
        userProvidedQuantityDelivered = BigDecimal("10"),
        unitCost = BigDecimal("5.0000"),
        unitId = UUID.randomUUID(),
        storageUnitsPerUserProvidedUnit = 12L,
    ).also {
        it.id = UUID.randomUUID()
        it.referenceNumber = "DELN000001"
    }

    private val purchaseLineDto = PurchaseLineDto(
        id = revisedLine.purchaseLineId,
        purchaseId = UUID.randomUUID(),
        locationProductId = UUID.randomUUID(),
        unitCost = BigDecimal("5.0000"),
        storageUnitsPerUserProvidedUnit = 12L,
        unitId = revisedLine.unitId,
        storageQuantityExpected = ExactQuantity.ofStorage(BigDecimal("240")),
        storageQuantityRemaining = ExactQuantity.ZERO,
    )

    private fun stubRepositories() {
        `when`(purchaseDeliveryLineRepository.getReferenceById(revisedLine.id!!)).thenReturn(revisedLine)
        val purchaseId = UUID.randomUUID()
        `when`(purchaseDeliveryRepository.getReferenceById(deliveryId)).thenReturn(
            PurchaseDeliveryEntity(
                purchaseId,
                deliveredAt = OffsetDateTime.now()
            ).also { it.referenceNumber = "DEL000001" }
        )
        `when`(deliveryHandlerForPurchase.getDeliveryContext(purchaseId)).thenReturn(
            PurchaseDeliveryContext(
                purchaseId,
                "PUR000001",
                UUID.randomUUID(),
                mapOf(revisedLine.purchaseLineId to purchaseLineDto)
            )
        )
        `when`(
            purchaseDeliveryLineRepository.save(any(PurchaseDeliveryLineEntity::class.java))
        ).thenAnswer { invocation ->
            invocation.getArgument<PurchaseDeliveryLineEntity>(0).also {
                if (it !== revisedLine) it.referenceNumber = "DELN000002"
            }
        }
    }

    @Test
    fun `the replacement is a sibling that keeps the purchase line and unit, and the revised line becomes REVISED`() {
        stubRepositories()

        val replacement = purchaseDeliveryLineReplacer.replace(
            PurchaseDeliveryLineReplaceRequest(
                revisedLine.id!!,
                UserProvidedQuantity.of(BigDecimal("0")),
                Money.of(BigDecimal("6.5000"))
            )
        )

        assertEquals(PurchaseDeliveryLineStatus.REVISED, revisedLine.status)
        assertEquals("DEL000001", replacement.deliveryReferenceNumber)
        assertEquals("DELN000001", replacement.revisedLineReferenceNumber)
        assertEquals("DELN000002", replacement.replacementLineReferenceNumber)
        assertEquals(revisedLine.purchaseLineId, replacement.purchaseLineId)
        assertEquals(revisedLine.unitId, replacement.unitId)
        assertEquals(12L, replacement.storageUnitsPerUserProvidedUnit)
        assertEquals(BigDecimal("0"), replacement.userProvidedQuantityDelivered)
        assertEquals(BigDecimal("6.50"), replacement.unitCost)
    }

    @Test
    fun `a line that fails revision validation is rejected before any row is written`() {
        stubRepositories()
        doThrow(RtsGenericException("already revised")).`when`(purchaseDeliveryRevisionValidator)
            .validate("DEL000001", purchaseLineDto, revisedLine, ExactQuantity.ofStorage(BigDecimal("48")))

        assertThrows(RtsGenericException::class.java) {
            purchaseDeliveryLineReplacer.replace(
                PurchaseDeliveryLineReplaceRequest(
                    revisedLine.id!!,
                    UserProvidedQuantity.of(BigDecimal("4")),
                    Money.of(BigDecimal("5.0000"))
                )
            )
        }

        verify(purchaseDeliveryLineRepository, never()).save(any(PurchaseDeliveryLineEntity::class.java))
        assertEquals(PurchaseDeliveryLineStatus.ACTIVE, revisedLine.status)
    }
}
