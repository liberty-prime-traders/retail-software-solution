package me.ezra_home.retail_software_solution.locations.business.purchase.api

import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseAssembler
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseEntity
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseLineEntity
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseLineRepository
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseRepository
import me.ezra_home.retail_software_solution.locations.business.purchase.api.delivery.PurchaseDeliveryRevisionApplier
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.api.PurchasePaymentStatusService
import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

class PurchaseDeliveryRevisionApplierTest {

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
    private val purchaseResponse = PurchaseResponseDto(
        id = purchaseId,
        referenceNumber = "PUR000001",
        supplierId = UUID.randomUUID(),
        supplierName = null,
        purchaseStatus = PurchaseStatus.ORDERED,
        paymentStatus = PaymentStatus.UNPAID,
        notes = null,
        dateOrdered = null,
        orderedBy = null,
        orderedById = null,
        createdBy = null,
        createdOn = null,
        lines = emptyList(),
        orderedTotal = BigDecimal.ZERO,
        deliveredTotal = BigDecimal.ZERO,
        paymentCeiling = BigDecimal("10.00"),
        deliveries = emptyList()
    )
    private val purchasePaymentStatusService = mock(PurchasePaymentStatusService::class.java)
    private val purchaseDeliveryRevisionApplier = PurchaseDeliveryRevisionApplier(
        purchaseRepository = mock(PurchaseRepository::class.java).also {
            `when`(it.getReferenceById(purchaseId)).thenReturn(purchase)
        },
        purchaseLineRepository = mock(PurchaseLineRepository::class.java).also {
            `when`(it.findByPurchaseId(purchaseId)).thenReturn(listOf(purchaseLine))
        },
        purchaseAssembler = mock(PurchaseAssembler::class.java).also {
            `when`(it.buildResponse(purchase, listOf(purchaseLine))).thenReturn(purchaseResponse)
        },
        purchasePaymentStatusService = purchasePaymentStatusService,
    )

    @BeforeEach
    fun assignLineIdAndPaymentStatus() {
        purchaseLine.id = purchaseLineId
        `when`(purchasePaymentStatusService.patchThenReturnPaymentStatus(purchaseId, BigDecimal("10.00")))
            .thenReturn(PaymentStatus.PARTIALLY_SETTLED)
    }

    private fun applyDelivered(storageQuantityDelivered: String): PurchaseResponseDto =
        purchaseDeliveryRevisionApplier.apply(
            purchaseId,
            purchaseLineId,
            ExactQuantity.ofStorage(BigDecimal(storageQuantityDelivered))
        )

    @Test
    fun `a revision sets the delivered quantity and reopens a fully delivered purchase`() {
        purchase.purchaseStatus = PurchaseStatus.FULLY_DELIVERED
        purchaseLine.storageQuantityDelivered = BigDecimal("18")

        applyDelivered("12")

        assertEquals(0, BigDecimal("12").compareTo(purchaseLine.storageQuantityDelivered))
        assertEquals(PurchaseStatus.PARTIALLY_DELIVERED, purchase.purchaseStatus)
    }

    @Test
    fun `a stored total that drifted is replaced by the computed one`() {
        purchaseLine.storageQuantityDelivered = BigDecimal("7")

        applyDelivered("12")

        assertEquals(0, BigDecimal("12").compareTo(purchaseLine.storageQuantityDelivered))
    }

    @Test
    fun `a revision to zero returns an undelivered purchase to ordered`() {
        purchase.purchaseStatus = PurchaseStatus.PARTIALLY_DELIVERED

        applyDelivered("0")

        assertEquals(0, BigDecimal.ZERO.compareTo(purchaseLine.storageQuantityDelivered))
        assertEquals(PurchaseStatus.ORDERED, purchase.purchaseStatus)
    }

    @Test
    fun `a revision upward can complete the line`() {
        applyDelivered("18")

        assertEquals(PurchaseStatus.FULLY_DELIVERED, purchase.purchaseStatus)
    }

    @Test
    fun `the payment status is patched with the ceiling the response already carries`() {
        val response = applyDelivered("18")

        verify(purchasePaymentStatusService).patchThenReturnPaymentStatus(purchaseId, BigDecimal("10.00"))
        assertEquals(PaymentStatus.PARTIALLY_SETTLED, response.paymentStatus)
    }
}
