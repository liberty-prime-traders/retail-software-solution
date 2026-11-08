package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryLineReplaceRequest
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryLineReplacer
import me.ezra_home.retail_software_solution.locations.business.delivery.api.ValidatedDeliveryLineRevisionDto
import me.ezra_home.retail_software_solution.locations.business.stock_revision.api.DeliveryLineRevisionRequest
import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductStockBreakdownProvider
import me.ezra_home.retail_software_solution.locations.business.stock.api.fifo.FifoLayer
import me.ezra_home.retail_software_solution.locations.business.stock.api.fifo.StockEntryFifoDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.read.DeliveryLineStockInspector
import me.ezra_home.retail_software_solution.locations.business.stock.api.revision.StockReplacementLayerPreviewer
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.exceptions.BlockingProblem
import me.ezra_home.retail_software_solution.util.quantity.BreakdownUnit
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityAsEntered
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import me.ezra_home.retail_software_solution.util.quantity.UserProvidedQuantity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class DeliveryLineRevisionAssessorTest {

    private val locationProductId = UUID.randomUUID()
    private val revisedDeliveryLineId = UUID.randomUUID()
    private val revisedStockEntryId = UUID.randomUUID()
    private val unitId = UUID.randomUUID()
    private val quantityBreakdown = QuantityBreakdown.forBaseUnit(
        listOf(BreakdownUnit(unitId, "pcs", 1L, unitId, OffsetDateTime.parse("2026-11-01T10:00:00Z"))), unitId
    )
    private val purchaseDeliveryLineReplacer = mock(PurchaseDeliveryLineReplacer::class.java)
    private val deliveryLineStockInspector = mock(DeliveryLineStockInspector::class.java)
    private val productStockBreakdownProvider = mock(ProductStockBreakdownProvider::class.java).also {
        `when`(it.forLocationProduct(locationProductId)).thenReturn(quantityBreakdown)
    }
    private val netConsumptionCalculator = mock(NetConsumptionCalculator::class.java)
    private val stockReplacementLayerPreviewer = mock(StockReplacementLayerPreviewer::class.java)
    private val replayPlanner = mock(ReplayPlanner::class.java)
    private val reservationShortfallChecker = mock(ReservationShortfallChecker::class.java)
    private val deliveryLineRevisionAssessor = DeliveryLineRevisionAssessor(
        purchaseDeliveryLineReplacer,
        deliveryLineStockInspector,
        productStockBreakdownProvider,
        netConsumptionCalculator,
        stockReplacementLayerPreviewer,
        replayPlanner,
        reservationShortfallChecker,
    )

    private val deliveryLineRevisionRequest = DeliveryLineRevisionRequest(
        revisedDeliveryLineId = revisedDeliveryLineId,
        userProvidedQuantityDelivered = UserProvidedQuantity.of(BigDecimal("8")),
        unitId = unitId,
        unitCost = Money.of(BigDecimal("12.0000")),
        reason = "Miscounted",
    )

    private fun givenValidation(problems: List<BlockingProblem>) {
        `when`(
            purchaseDeliveryLineReplacer.validateRevisionReportingProblems(
                PurchaseDeliveryLineReplaceRequest(
                    revisedDeliveryLineId,
                    UserProvidedQuantity.of(BigDecimal("8")),
                    unitId,
                    Money.of(BigDecimal("12.0000"))
                )
            )
        ).thenReturn(
            ValidatedDeliveryLineRevisionDto(
                revisedDeliveryLineId = revisedDeliveryLineId,
                deliveryReferenceNumber = "DEL000001",
                revisedLineReferenceNumber = "DELN000001",
                purchaseLineId = UUID.randomUUID(),
                locationProductId = locationProductId,
                revisedUnitId = unitId,
                revisedStorageUnitsPerUserProvidedUnit = 1L,
                unitId = unitId,
                storageUnitsPerUserProvidedUnit = 1L,
                revisedUserProvidedQuantityDelivered = BigDecimal("10"),
                revisedUnitCost = BigDecimal("10.0000"),
                userProvidedQuantityDelivered = BigDecimal("8"),
                unitCost = BigDecimal("12.0000"),
                problems = problems,
            )
        )
    }

    @Test
    fun `a failed check is reported and nothing is replayed`() {
        givenValidation(listOf(BlockingProblem("DELN000001", "Already changed.", "Change its latest line.")))

        val assessment = deliveryLineRevisionAssessor.assess(deliveryLineRevisionRequest)

        assertEquals(listOf("DELN000001"), assessment.blockingProblems.map { it.documentReference })
        verifyNoInteractions(replayPlanner, netConsumptionCalculator, stockReplacementLayerPreviewer)
    }

    private fun previewReplacementLayerRequest() = DeliveryReplacementLayerRequests.of(
        locationProductId = locationProductId,
        deliveryReferenceNumber = "DEL000001",
        replacementLineReferenceNumber = "PREVIEW",
        stockRevisionReferenceNumber = "PREVIEW",
        userProvidedQuantityDelivered = BigDecimal("8"),
        unitId = unitId,
        storageUnitsPerUserProvidedUnit = 1L,
        unitCost = BigDecimal("12.0000"),
        revisedStockEntryId = revisedStockEntryId,
    )

    private fun givenPassingRevisionWithReplay(replayResult: ReplayResult) {
        givenValidation(emptyList())
        `when`(deliveryLineStockInspector.getStockEntryId("DEL000001", "DELN000001")).thenReturn(revisedStockEntryId)
        `when`(netConsumptionCalculator.calculateNetConsumptions(revisedStockEntryId)).thenReturn(emptyList())
        val layersAfterReplacement = emptyList<StockEntryFifoDto>()
        `when`(
            stockReplacementLayerPreviewer.fifoEntriesAfterReplacement(previewReplacementLayerRequest())
        ).thenReturn(layersAfterReplacement)
        `when`(replayPlanner.planReplayForPreview(emptyList(), layersAfterReplacement, "PREVIEW", quantityBreakdown))
            .thenReturn(replayResult)
    }

    @Test
    fun `a revision that passes its checks is replayed against the layers after the replacement`() {
        givenPassingRevisionWithReplay(ReplayResult(emptyList(), emptyList()))

        val assessment = deliveryLineRevisionAssessor.assess(deliveryLineRevisionRequest)

        assertTrue(assessment.blockingProblems.isEmpty())
    }

    @Test
    fun `a consumer no ledger restatement covers blocks the revision and nothing is replayed`() {
        givenPassingRevisionWithReplay(ReplayResult(emptyList(), emptyList()))
        `when`(netConsumptionCalculator.calculateNetConsumptions(revisedStockEntryId)).thenReturn(
            listOf(
                StockEntryConsumption(
                    stockEntryId = revisedStockEntryId,
                    locationProductId = locationProductId,
                    consumer = StockConsumer(StockMovementOrigin("TRF1", "TRFL1"), MovementType.TRANSFER_OUT, null),
                    quantityAsEntered = QuantityAsEntered(unitId, 1L, "whole line"),
                    orderOfConsumption = OrderOfConsumption(null, null),
                    netConsumedStorageQuantity = ExactQuantity.ofStorage(BigDecimal.ONE),
                    netConsumedCost = BigDecimal("10.00"),
                )
            )
        )

        val assessment = deliveryLineRevisionAssessor.assess(deliveryLineRevisionRequest)

        assertEquals(listOf("TRF1"), assessment.blockingProblems.map { it.documentReference })
        verifyNoInteractions(replayPlanner, reservationShortfallChecker)
    }

    @Test
    fun `the replacement layer is the one the preview labelled with its own reference`() {
        givenValidation(emptyList())
        `when`(deliveryLineStockInspector.getStockEntryId("DEL000001", "DELN000001")).thenReturn(revisedStockEntryId)
        `when`(netConsumptionCalculator.calculateNetConsumptions(revisedStockEntryId)).thenReturn(emptyList())
        fun fifoLayerOf(referenceNumber: String) = StockEntryFifoDto(
            referenceNumber, locationProductId, BigDecimal.ONE,
            FifoLayer(UUID.randomUUID(), BigDecimal.TEN, BigDecimal("100.00"), BigDecimal.TEN, BigDecimal("100.00"))
        )
        val laterDelivery = fifoLayerOf("STE000009")
        val replacementLayer = fifoLayerOf("PREVIEW")
        val layersAfterReplacement = listOf(replacementLayer, laterDelivery)
        `when`(stockReplacementLayerPreviewer.fifoEntriesAfterReplacement(previewReplacementLayerRequest()))
            .thenReturn(layersAfterReplacement)
        `when`(replayPlanner.planReplayForPreview(emptyList(), layersAfterReplacement, "PREVIEW", quantityBreakdown))
            .thenReturn(ReplayResult(emptyList(), emptyList()))

        val assessment = deliveryLineRevisionAssessor.assess(deliveryLineRevisionRequest)

        assertEquals(replacementLayer.fifoLayer.stockEntryId, assessment.replacementStockEntryId)
    }

    @Test
    fun `draft sales left short by the revision are blocking rows`() {
        givenPassingRevisionWithReplay(ReplayResult(emptyList(), emptyList()))
        `when`(
            reservationShortfallChecker.shortfallAfterPendingChange(
                locationProductId, ExactQuantity.ofStorage(BigDecimal("-2")), quantityBreakdown
            )
        ).thenReturn(
            ReservationShortfall(
                listOf(BlockingProblem("SAL000420 (draft)", "Draft sale holding 4 pcs of Sugar.", "Remove it.")),
                "Sugar", ExactQuantity.ofStorage(BigDecimal("8")), ExactQuantity.ofStorage(BigDecimal("10"))
            )
        )

        val assessment = deliveryLineRevisionAssessor.assess(deliveryLineRevisionRequest)

        assertEquals(listOf("SAL000420 (draft)"), assessment.blockingProblems.map { it.documentReference })
    }

    @Test
    fun `reservations are not checked while a replay shortfall already blocks the save`() {
        givenPassingRevisionWithReplay(
            ReplayResult(emptyList(), listOf(BlockingProblem("SAL1", "Sale that took 4 pcs.", "Enter more.")))
        )

        val assessment = deliveryLineRevisionAssessor.assess(deliveryLineRevisionRequest)

        assertEquals(listOf("SAL1"), assessment.blockingProblems.map { it.documentReference })
        verifyNoInteractions(reservationShortfallChecker)
    }
}
