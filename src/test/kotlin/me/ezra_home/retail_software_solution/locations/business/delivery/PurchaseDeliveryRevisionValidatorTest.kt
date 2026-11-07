package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductStockBreakdownProvider
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDeliveryLineStatus
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.read.DeliveryLineStockInspector
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

class PurchaseDeliveryRevisionValidatorTest {

    private val locationProductId = UUID.randomUUID()
    private val deliveryReferenceNumber = "DLV000001"

    private val locationProductDataFetcher = mock(LocationProductDataFetcher::class.java).also {
        val summary = mock(LocationProductSummaryDto::class.java)
        `when`(summary.label).thenReturn("Sugar")
        `when`(it.findSummaryByIds(setOf(locationProductId))).thenReturn(mapOf(locationProductId to summary))
    }
    private val productStockBreakdownProvider = mock(ProductStockBreakdownProvider::class.java).also {
        `when`(it.forLocationProducts(setOf(locationProductId)))
            .thenReturn(mapOf(locationProductId to mock(QuantityBreakdown::class.java)))
    }
    private val deliveryLineStockInspector = mock(DeliveryLineStockInspector::class.java)
    private val purchaseDeliveryRevisionValidator = PurchaseDeliveryRevisionValidator(
        PurchaseDeliveryValidator(locationProductDataFetcher, productStockBreakdownProvider), deliveryLineStockInspector
    )

    private fun deliveryLine(
        quantity: String,
        status: PurchaseDeliveryLineStatus = PurchaseDeliveryLineStatus.ACTIVE
    ) = PurchaseDeliveryLineEntity(
        purchaseDeliveryId = UUID.randomUUID(),
        purchaseLineId = UUID.randomUUID(),
        userProvidedQuantityDelivered = BigDecimal(quantity),
        unitCost = BigDecimal("10.00"),
        unitId = UUID.randomUUID(),
        storageUnitsPerUserProvidedUnit = 1L,
        status = status
    ).also { it.referenceNumber = "DLL000001" }

    private fun purchaseLine(storageQuantityRemaining: String) = PurchaseLineDto(
        id = UUID.randomUUID(),
        purchaseId = UUID.randomUUID(),
        locationProductId = locationProductId,
        unitCost = BigDecimal("10.00"),
        storageUnitsPerUserProvidedUnit = 1L,
        unitId = UUID.randomUUID(),
        storageQuantityExpected = ExactQuantity.ofStorage(BigDecimal("10")),
        storageQuantityRemaining = ExactQuantity.ofStorage(BigDecimal(storageQuantityRemaining))
    )

    @Test
    fun `an active line can be revised`() {
        assertDoesNotThrow { purchaseDeliveryRevisionValidator.ensureLineIsActive(deliveryLine("4")) }
    }

    @Test
    fun `a revised line cannot be revised again, so a chain only grows from its active end`() {
        assertThrows(RtsGenericException::class.java) {
            purchaseDeliveryRevisionValidator.ensureLineIsActive(deliveryLine("4", PurchaseDeliveryLineStatus.REVISED))
        }
    }

    @Test
    fun `a cost-only revision repeating the delivered quantity fits even when nothing is left to deliver`() {
        assertDoesNotThrow {
            purchaseDeliveryRevisionValidator.ensurePendingDeliveryQtyIsNotExceeded(
                purchaseLine("0"), deliveryLine("4"), ExactQuantity.ofStorage(BigDecimal("4"))
            )
        }
    }

    @Test
    fun `the replacement may use the revised line's quantity plus what is still remaining`() {
        assertDoesNotThrow {
            purchaseDeliveryRevisionValidator.ensurePendingDeliveryQtyIsNotExceeded(
                purchaseLine("2"), deliveryLine("4"), ExactQuantity.ofStorage(BigDecimal("6"))
            )
        }
    }

    @Test
    fun `a replacement above the net remaining quantity is rejected`() {
        assertThrows(RtsGenericException::class.java) {
            purchaseDeliveryRevisionValidator.ensurePendingDeliveryQtyIsNotExceeded(
                purchaseLine("2"), deliveryLine("4"), ExactQuantity.ofStorage(BigDecimal("7"))
            )
        }
    }

    @Test
    fun `a line with transferred-out stock cannot be revised`() {
        `when`(deliveryLineStockInspector.hasTransferredOut(deliveryReferenceNumber, "DLL000001")).thenReturn(true)

        assertThrows(RtsGenericException::class.java) {
            purchaseDeliveryRevisionValidator.ensureNoTransferredStock(deliveryReferenceNumber, deliveryLine("4"))
        }
    }

    @Test
    fun `stock still at the source does not block a revision`() {
        `when`(deliveryLineStockInspector.hasTransferredOut(deliveryReferenceNumber, "DLL000001")).thenReturn(false)

        assertDoesNotThrow {
            purchaseDeliveryRevisionValidator.ensureNoTransferredStock(deliveryReferenceNumber, deliveryLine("4"))
        }
    }

    @Test
    fun `a line whose stock has a movement that cannot be reversed cannot be revised`() {
        `when`(
            deliveryLineStockInspector.hasMovementsThatCannotBeReversed(deliveryReferenceNumber, "DLL000001")
        ).thenReturn(true)

        assertThrows(RtsGenericException::class.java) {
            purchaseDeliveryRevisionValidator.ensureNoMovementsThatCannotBeReversed(
                deliveryReferenceNumber,
                deliveryLine("4")
            )
        }
    }

    @Test
    fun `a replacement of zero is allowed`() {
        assertDoesNotThrow {
            purchaseDeliveryRevisionValidator.ensurePendingDeliveryQtyIsNotExceeded(
                purchaseLine("2"), deliveryLine("4"), ExactQuantity.ZERO
            )
        }
    }

    @Test
    fun `validate passes when every check passes`() {
        assertDoesNotThrow {
            purchaseDeliveryRevisionValidator.validate(
                deliveryReferenceNumber, purchaseLine("2"), deliveryLine("4"), ExactQuantity.ofStorage(BigDecimal("6"))
            )
        }
    }

    @Test
    fun `validate collects every failing check into one error list`() {
        `when`(deliveryLineStockInspector.hasTransferredOut(deliveryReferenceNumber, "DLL000001")).thenReturn(true)
        `when`(
            deliveryLineStockInspector.hasMovementsThatCannotBeReversed(deliveryReferenceNumber, "DLL000001")
        ).thenReturn(true)

        val exception = assertThrows(RtsGenericException::class.java) {
            purchaseDeliveryRevisionValidator.validate(
                deliveryReferenceNumber,
                purchaseLine("2"),
                deliveryLine("4", PurchaseDeliveryLineStatus.REVISED),
                ExactQuantity.ofStorage(BigDecimal("7"))
            )
        }

        assertEquals(4, (exception.payload as List<*>).size)
    }
}
