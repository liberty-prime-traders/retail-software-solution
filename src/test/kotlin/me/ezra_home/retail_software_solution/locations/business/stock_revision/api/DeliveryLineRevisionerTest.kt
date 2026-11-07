package me.ezra_home.retail_software_solution.locations.business.stock_revision.api

import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryDataFetcher
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryLineReplaceRequest
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryLineReplacer
import me.ezra_home.retail_software_solution.locations.business.delivery.api.ReplacementDeliveryLineDto
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDataFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.ConsumerReplayDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.DeliveryLineStockInspector
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBreakdownProvider
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockReplacementLayerRecorder
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockReplacementLayerRequest
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockRevisionCommitter
import me.ezra_home.retail_software_solution.locations.business.stock.api.ConsumerReplayPlan
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockEntryResetPlan
import me.ezra_home.retail_software_solution.locations.business.stock_revision.NetConsumptionCalculator
import me.ezra_home.retail_software_solution.locations.business.stock_revision.ReplayPlanner
import me.ezra_home.retail_software_solution.locations.business.stock_revision.ResetPlanner
import me.ezra_home.retail_software_solution.locations.business.stock_revision.StockEntryConsumption
import me.ezra_home.retail_software_solution.locations.business.stock_revision.StockRevisionEntity
import me.ezra_home.retail_software_solution.locations.business.stock_revision.StockRevisionRepository
import me.ezra_home.retail_software_solution.locations.business.stock_revision.StockRevisionSourceType
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.business.reference_number.ReferenceNumberGenerator
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.quantity.BreakdownUnit
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import me.ezra_home.retail_software_solution.util.quantity.UserProvidedQuantity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class DeliveryLineRevisionerTest {

    private val purchaseDeliveryDataFetcher = mock(PurchaseDeliveryDataFetcher::class.java)
    private val purchaseDataFetcher = mock(PurchaseDataFetcher::class.java)
    private val purchaseDeliveryLineReplacer = mock(PurchaseDeliveryLineReplacer::class.java)
    private val deliveryLineStockInspector = mock(DeliveryLineStockInspector::class.java)
    private val stockRevisionCommitter = mock(StockRevisionCommitter::class.java)
    private val stockReplacementLayerRecorder = mock(StockReplacementLayerRecorder::class.java)
    private val netConsumptionCalculator = mock(NetConsumptionCalculator::class.java)
    private val resetPlanner = mock(ResetPlanner::class.java)
    private val replayPlanner = mock(ReplayPlanner::class.java)
    private val entityAdvisoryLock = mock(EntityAdvisoryLock::class.java)
    private val referenceNumberGenerator = mock(ReferenceNumberGenerator::class.java)
    private val stockRevisionRepository = mock(StockRevisionRepository::class.java)
    private val stockBreakdownProvider = mock(StockBreakdownProvider::class.java)
    private val deliveryLineRevisioner = DeliveryLineRevisioner(
        purchaseDeliveryDataFetcher, purchaseDataFetcher, purchaseDeliveryLineReplacer, deliveryLineStockInspector, stockRevisionCommitter,
        stockReplacementLayerRecorder, netConsumptionCalculator, resetPlanner, replayPlanner, entityAdvisoryLock, referenceNumberGenerator,
        stockRevisionRepository, stockBreakdownProvider
    )

    private val revisedDeliveryLineId = UUID.randomUUID()
    private val purchaseId = UUID.randomUUID()
    private val revisedPurchaseLineId = UUID.randomUUID()
    private val locationProductId = UUID.randomUUID()
    private val revisedStockEntryId = UUID.randomUUID()
    private val replacementStockEntryId = UUID.randomUUID()
    private val unitId = UUID.randomUUID()
    private val quantityBreakdown = QuantityBreakdown.forBaseUnit(listOf(BreakdownUnit(unitId, "pcs", 1L, unitId, OffsetDateTime.now())), unitId)
    private val replacementLine = ReplacementDeliveryLineDto(
        deliveryReferenceNumber = "DEL000001",
        revisedLineReferenceNumber = "DELN000001",
        replacementLineReferenceNumber = "DELN000002",
        purchaseLineId = UUID.randomUUID(),
        userProvidedQuantityDelivered = BigDecimal("4"),
        unitCost = BigDecimal("6.50"),
        unitId = unitId,
        storageUnitsPerUserProvidedUnit = 12L,
    )
    private val consumerReplay = ConsumerReplayDto(locationProductId, MovementType.SALE, "SAL000001", "SALN000001", BigDecimal("30.00"), BigDecimal("39.00"))
    private val consumptionsOnRevisedLayer = emptyList<StockEntryConsumption>()
    private val stockEntryResetPlan = StockEntryResetPlan(revisedStockEntryId, locationProductId, emptyList())
    private val consumerReplayPlans = emptyList<ConsumerReplayPlan>()

    private fun request(reason: String = "Supplier corrected the invoice") = DeliveryLineRevisionRequest(
        revisedDeliveryLineId, UserProvidedQuantity.of(BigDecimal("4")), Money.of(BigDecimal("6.5000")), reason
    )

    private fun stubCollaborators() {
        `when`(purchaseDeliveryDataFetcher.getPurchaseLineId(revisedDeliveryLineId)).thenReturn(revisedPurchaseLineId)
        `when`(purchaseDataFetcher.getLocationProductIdOfLine(revisedPurchaseLineId)).thenReturn(locationProductId)
        `when`(purchaseDeliveryDataFetcher.getPurchaseId(revisedDeliveryLineId)).thenReturn(purchaseId)
        `when`(referenceNumberGenerator.generateSingle(TableName.STOCK_REVISION)).thenReturn("STRV000001")
        `when`(purchaseDeliveryLineReplacer.replace(any(PurchaseDeliveryLineReplaceRequest::class.java) ?: PurchaseDeliveryLineReplaceRequest(revisedDeliveryLineId, UserProvidedQuantity.of(BigDecimal("4")), Money.of(BigDecimal("6.5000")))))
            .thenReturn(replacementLine)
        `when`(deliveryLineStockInspector.getStockEntryId("DEL000001", "DELN000001")).thenReturn(revisedStockEntryId)
        `when`(netConsumptionCalculator.calculateNetConsumptions(revisedStockEntryId)).thenReturn(consumptionsOnRevisedLayer)
        `when`(stockBreakdownProvider.forLocationProducts(listOf(locationProductId))).thenReturn(mapOf(locationProductId to quantityBreakdown))
        `when`(resetPlanner.plan(revisedStockEntryId, consumptionsOnRevisedLayer, "STRV000001", quantityBreakdown)).thenReturn(stockEntryResetPlan)
        `when`(replayPlanner.plan(consumptionsOnRevisedLayer, "STRV000001", quantityBreakdown)).thenReturn(consumerReplayPlans)
        `when`(stockReplacementLayerRecorder.record(any(StockReplacementLayerRequest::class.java) ?: layerRequestPlaceholder()))
            .thenReturn(replacementStockEntryId)
        `when`(stockRevisionCommitter.commitReplay(consumerReplayPlans, replacementStockEntryId, quantityBreakdown))
            .thenReturn(listOf(consumerReplay))
    }

    private fun layerRequestPlaceholder() = StockReplacementLayerRequest(
        locationProductId, StockItemSource.PURCHASE, MovementType.PURCHASE_RECEIVED, "", "", "", BigDecimal.ZERO, unitId, 1L, BigDecimal.ZERO, revisedStockEntryId
    )

    @Test
    fun `a revision locks the product, then replaces the line, resets the layer, records the replacement and replays, and saves the revision last`() {
        stubCollaborators()

        val response = deliveryLineRevisioner.revise(request())

        val ordered = inOrder(
            purchaseDataFetcher, entityAdvisoryLock, purchaseDeliveryLineReplacer, stockRevisionCommitter, stockReplacementLayerRecorder, stockRevisionRepository
        )
        ordered.verify(purchaseDataFetcher).lockPurchase(purchaseId)
        ordered.verify(entityAdvisoryLock).acquire(LockNamespaces.PRODUCT, locationProductId)
        ordered.verify(purchaseDeliveryLineReplacer).replace(any(PurchaseDeliveryLineReplaceRequest::class.java) ?: PurchaseDeliveryLineReplaceRequest(revisedDeliveryLineId, UserProvidedQuantity.of(BigDecimal("4")), Money.of(BigDecimal("6.5000"))))
        ordered.verify(stockRevisionCommitter).commitReset(stockEntryResetPlan, quantityBreakdown)
        ordered.verify(stockReplacementLayerRecorder).record(any(StockReplacementLayerRequest::class.java) ?: layerRequestPlaceholder())
        ordered.verify(stockRevisionCommitter).commitReplay(consumerReplayPlans, replacementStockEntryId, quantityBreakdown)
        ordered.verify(stockRevisionRepository).save(any(StockRevisionEntity::class.java) ?: unusedRevisionEntity())
        assertEquals("STRV000001", response.stockRevisionReferenceNumber)
        assertEquals(listOf(consumerReplay), response.consumerReplays)
        assertEquals(replacementLine, response.replacementDeliveryLine)
    }

    @Test
    fun `the replacement layer keeps the delivery document, takes the new line as detail and costs the whole batch`() {
        stubCollaborators()

        deliveryLineRevisioner.revise(request())

        val layerRequestCaptor = ArgumentCaptor.forClass(StockReplacementLayerRequest::class.java)
        verify(stockReplacementLayerRecorder).record(layerRequestCaptor.capture() ?: layerRequestPlaceholder())
        val layerRequest = layerRequestCaptor.value
        assertEquals("DEL000001", layerRequest.documentReference)
        assertEquals("DELN000002", layerRequest.detailReference)
        assertEquals("STRV000001", layerRequest.stockRevisionReferenceNumber)
        assertEquals(StockItemSource.PURCHASE, layerRequest.sourceType)
        assertEquals(MovementType.PURCHASE_RECEIVED, layerRequest.movementType)
        assertEquals(BigDecimal("26.00"), layerRequest.batchCost)
        assertEquals(12L, layerRequest.storageUnitsPerUserProvidedUnit)
    }

    @Test
    fun `the stock revision links the revised and replacement layers under the drawn reference`() {
        stubCollaborators()

        deliveryLineRevisioner.revise(request())

        val revisionCaptor = ArgumentCaptor.forClass(StockRevisionEntity::class.java)
        verify(stockRevisionRepository).save(revisionCaptor.capture() ?: unusedRevisionEntity())
        val savedRevision = revisionCaptor.value
        assertEquals("STRV000001", savedRevision.referenceNumber)
        assertEquals(StockRevisionSourceType.DELIVERY_LINE, savedRevision.sourceType)
        assertEquals("DELN000002", savedRevision.sourceReferenceNumber)
        assertEquals(revisedStockEntryId, savedRevision.revisedStockEntryId)
        assertEquals(replacementStockEntryId, savedRevision.replacementStockEntryId)
        assertEquals("Supplier corrected the invoice", savedRevision.reason)
    }

    @Test
    fun `the replacement layer is recorded in the place of the revised layer`() {
        stubCollaborators()

        deliveryLineRevisioner.revise(request())

        val layerRequestCaptor = ArgumentCaptor.forClass(StockReplacementLayerRequest::class.java)
        verify(stockReplacementLayerRecorder).record(layerRequestCaptor.capture() ?: layerRequestPlaceholder())
        assertEquals(revisedStockEntryId, layerRequestCaptor.value.revisedStockEntryId)
    }

    @Test
    fun `a blank reason is refused before anything is read or locked`() {
        assertThrows(RtsGenericException::class.java) { deliveryLineRevisioner.revise(request(reason = "  ")) }

        verifyNoInteractions(purchaseDeliveryDataFetcher, entityAdvisoryLock, stockRevisionCommitter)
    }

    private fun unusedRevisionEntity() = StockRevisionEntity(StockRevisionSourceType.DELIVERY_LINE, "", UUID.randomUUID(), UUID.randomUUID(), "")
}
