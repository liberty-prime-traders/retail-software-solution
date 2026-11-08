package me.ezra_home.retail_software_solution.locations.business.stock_revision.api

import me.ezra_home.retail_software_solution.locations.business.delivery.api.DeliveryLineOwnersDto
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryDataFetcher
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryLineReplacer
import me.ezra_home.retail_software_solution.locations.business.delivery.api.ReplacementDeliveryLineDto
import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductStockBreakdownProvider
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDataFetcher
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDeliveryRevisionApplier
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseResponseDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseStatus
import me.ezra_home.retail_software_solution.locations.business.stock.api.read.DeliveryLineStockInspector
import me.ezra_home.retail_software_solution.locations.business.stock.api.revision.StockReplacementLayerRecorder
import me.ezra_home.retail_software_solution.locations.business.stock.api.revision.StockReplacementLayerRequest
import me.ezra_home.retail_software_solution.locations.business.stock.api.revision.StockRevisionCommitter
import me.ezra_home.retail_software_solution.locations.business.stock.api.revision.ConsumerReplayPlan
import me.ezra_home.retail_software_solution.locations.business.stock.api.revision.StockEntryResetPlan
import me.ezra_home.retail_software_solution.locations.business.stock_revision.DeliveryLineStockReviser
import me.ezra_home.retail_software_solution.locations.business.stock_revision.NetConsumptionCalculator
import me.ezra_home.retail_software_solution.locations.business.stock_revision.ReplayPlanner
import me.ezra_home.retail_software_solution.locations.business.stock_revision.ResetPlanner
import me.ezra_home.retail_software_solution.locations.business.stock_revision.RevisionCostPropagator
import me.ezra_home.retail_software_solution.locations.business.stock_revision.OrderOfConsumption
import me.ezra_home.retail_software_solution.locations.business.stock_revision.StockEntryConsumption
import me.ezra_home.retail_software_solution.locations.business.stock_revision.StockConsumer
import me.ezra_home.retail_software_solution.locations.business.stock_revision.StockMovementOrigin
import me.ezra_home.retail_software_solution.locations.business.stock_revision.StockRevisionEntity
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleCostRestater
import me.ezra_home.retail_software_solution.locations.business.stock_revision.SaleCostRestatementHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.stock_revision.StockAdjustmentRestatementHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.stock_revision.StockRevisionHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.stock_revision.StockRevisionRepository
import me.ezra_home.retail_software_solution.locations.business.stock_revision.StockRevisionSourceType
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.business.reference_number.ReferenceNumberGenerator
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.quantity.BreakdownUnit
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityAsEntered
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import me.ezra_home.retail_software_solution.util.quantity.UserProvidedQuantity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import me.ezra_home.retail_software_solution.util.exceptions.BlockedChangeException
import me.ezra_home.retail_software_solution.util.exceptions.BlockingProblem
import me.ezra_home.retail_software_solution.locations.business.stock_revision.DeliveryLineRevisionAssessment
import me.ezra_home.retail_software_solution.locations.business.stock_revision.DeliveryLineRevisionAssessor
import me.ezra_home.retail_software_solution.locations.business.stock_revision.ReplayResult
import me.ezra_home.retail_software_solution.locations.business.delivery.api.ValidatedDeliveryLineRevisionDto
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class DeliveryLineRevisionInitiatorTest {

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
    private val productStockBreakdownProvider = mock(ProductStockBreakdownProvider::class.java)
    private val purchaseDeliveryRevisionApplier = mock(PurchaseDeliveryRevisionApplier::class.java)
    private val deliveryLineRevisionAssessor = mock(DeliveryLineRevisionAssessor::class.java)
    private val stockRevisionHandlerForKafka = mock(StockRevisionHandlerForKafka::class.java)
    private val saleCostRestater = mock(SaleCostRestater::class.java)
    private val saleCostRestatementHandlerForKafka = mock(SaleCostRestatementHandlerForKafka::class.java)
    private val stockAdjustmentRestatementHandlerForKafka = mock(StockAdjustmentRestatementHandlerForKafka::class.java)
    private val deliveryLineStockReviser = DeliveryLineStockReviser(
        deliveryLineStockInspector,
        stockRevisionCommitter,
        stockReplacementLayerRecorder,
        netConsumptionCalculator,
        resetPlanner,
        replayPlanner,
        referenceNumberGenerator,
        stockRevisionRepository,
        productStockBreakdownProvider
    )
    private val revisionCostPropagator = RevisionCostPropagator(
        stockRevisionHandlerForKafka,
        saleCostRestater,
        saleCostRestatementHandlerForKafka,
        stockAdjustmentRestatementHandlerForKafka
    )
    private val deliveryLineRevisionInitiator = DeliveryLineRevisionInitiator(
        purchaseDeliveryDataFetcher,
        purchaseDataFetcher,
        purchaseDeliveryLineReplacer,
        entityAdvisoryLock,
        purchaseDeliveryRevisionApplier,
        deliveryLineRevisionAssessor,
        deliveryLineStockReviser,
        revisionCostPropagator
    )

    private val revisedDeliveryLineId = UUID.randomUUID()
    private val purchaseId = UUID.randomUUID()
    private val revisedPurchaseLineId = UUID.randomUUID()
    private val locationProductId = UUID.randomUUID()
    private val revisedStockEntryId = UUID.randomUUID()
    private val replacementStockEntryId = UUID.randomUUID()
    private val unitId = UUID.randomUUID()
    private val quantityBreakdown = QuantityBreakdown.forBaseUnit(
        listOf(BreakdownUnit(unitId, "pcs", 1L, unitId, OffsetDateTime.now())),
        unitId
    )
    private val replacementLine = ReplacementDeliveryLineDto(
        deliveryReferenceNumber = "DEL000001",
        revisedLineReferenceNumber = "DELN000001",
        replacementLineReferenceNumber = "DELN000002",
        purchaseLineId = revisedPurchaseLineId,
        userProvidedQuantityDelivered = BigDecimal("4"),
        unitCost = BigDecimal("6.50"),
        unitId = unitId,
        storageUnitsPerUserProvidedUnit = 12L,
        revisedLineTotal = BigDecimal("100.00"),
        replacementLineTotal = BigDecimal("26.00"),
    )
    private val validatedRevision = ValidatedDeliveryLineRevisionDto(
        revisedDeliveryLineId = revisedDeliveryLineId,
        deliveryReferenceNumber = "DEL000001",
        revisedLineReferenceNumber = "DELN000001",
        purchaseLineId = revisedPurchaseLineId,
        locationProductId = locationProductId,
        revisedUnitId = unitId,
        revisedStorageUnitsPerUserProvidedUnit = 12L,
        unitId = unitId,
        storageUnitsPerUserProvidedUnit = 12L,
        revisedUserProvidedQuantityDelivered = BigDecimal("3"),
        revisedUnitCost = BigDecimal("6.00"),
        userProvidedQuantityDelivered = BigDecimal("4"),
        unitCost = BigDecimal("6.50"),
        problems = emptyList(),
    )
    private val passingAssessment = DeliveryLineRevisionAssessment(
        validatedRevision, quantityBreakdown, ReplayResult(emptyList(), emptyList()), emptyList()
    )
    private val deliveredStorageQuantity = ExactQuantity.ofStorage(BigDecimal("30"))
    private val purchaseResponse = PurchaseResponseDto(
        id = purchaseId,
        referenceNumber = "PUR000001",
        supplierId = UUID.randomUUID(),
        supplierName = null,
        purchaseStatus = PurchaseStatus.PARTIALLY_DELIVERED,
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
        paymentCeiling = BigDecimal.ZERO,
        deliveries = emptyList()
    )
    private val consumptionsOnRevisedLayer = emptyList<StockEntryConsumption>()
    private val stockEntryResetPlan = StockEntryResetPlan(revisedStockEntryId, locationProductId, emptyList())
    private val consumerReplayPlans = emptyList<ConsumerReplayPlan>()

    private fun request(reason: String = "Supplier corrected the invoice") = DeliveryLineRevisionRequest(
        revisedDeliveryLineId, UserProvidedQuantity.of(BigDecimal("4")), unitId, Money.of(BigDecimal("6.5000")), reason
    )

    private fun stubCollaborators() {
        `when`(stockRevisionRepository.save(any(StockRevisionEntity::class.java) ?: unusedRevisionEntity()))
            .thenAnswer { it.arguments[0] }
        `when`(purchaseDataFetcher.getLocationProductIdOfLine(revisedPurchaseLineId)).thenReturn(locationProductId)
        `when`(purchaseDeliveryDataFetcher.getOwners(revisedDeliveryLineId))
            .thenReturn(DeliveryLineOwnersDto(purchaseId, revisedPurchaseLineId))
        `when`(referenceNumberGenerator.generateSingle(TableName.STOCK_REVISION)).thenReturn("STRV000001")
        `when`(deliveryLineRevisionAssessor.assess(request())).thenReturn(passingAssessment)
        `when`(purchaseDeliveryLineReplacer.replace(validatedRevision)).thenReturn(replacementLine)
        `when`(deliveryLineStockInspector.getStockEntryId("DEL000001", "DELN000001")).thenReturn(revisedStockEntryId)
        `when`(
            netConsumptionCalculator.calculateNetConsumptions(revisedStockEntryId)
        ).thenReturn(consumptionsOnRevisedLayer)
        `when`(productStockBreakdownProvider.forLocationProduct(locationProductId)).thenReturn(quantityBreakdown)
        `when`(
            resetPlanner.plan(revisedStockEntryId, consumptionsOnRevisedLayer, "STRV000001", quantityBreakdown)
        ).thenReturn(stockEntryResetPlan)
        `when`(
            replayPlanner.planReplayForExecution(consumptionsOnRevisedLayer, "STRV000001", quantityBreakdown)
        ).thenReturn(consumerReplayPlans)
        `when`(
            stockReplacementLayerRecorder.record(
                any(StockReplacementLayerRequest::class.java) ?: layerRequestPlaceholder()
            )
        )
            .thenReturn(replacementStockEntryId)
        `when`(purchaseDeliveryDataFetcher.calculateStorageQuantityDelivered(replacementLine.purchaseLineId))
            .thenReturn(deliveredStorageQuantity)
        `when`(
            purchaseDeliveryRevisionApplier.apply(
                purchaseId,
                replacementLine.purchaseLineId,
                deliveredStorageQuantity
            )
        ).thenReturn(purchaseResponse)
    }

    private fun layerRequestPlaceholder() = StockReplacementLayerRequest(
        locationProductId,
        StockItemSource.PURCHASE,
        MovementType.PURCHASE_RECEIVED,
        "",
        "",
        "",
        BigDecimal.ZERO,
        unitId,
        1L,
        BigDecimal.ZERO,
        revisedStockEntryId
    )

    @Test
    fun `a revision locks, replaces the line, resets, records the replacement, replays and saves the revision last`() {
        stubCollaborators()

        val response = deliveryLineRevisionInitiator.initiateDeliveryLineRevision(request())

        val ordered = inOrder(
            purchaseDataFetcher,
            entityAdvisoryLock,
            deliveryLineRevisionAssessor,
            purchaseDeliveryLineReplacer,
            stockRevisionCommitter,
            stockReplacementLayerRecorder,
            stockRevisionRepository,
            purchaseDeliveryRevisionApplier
        )
        ordered.verify(purchaseDataFetcher).lockPurchase(purchaseId)
        ordered.verify(entityAdvisoryLock).acquire(LockNamespaces.PRODUCT, locationProductId)
        ordered.verify(deliveryLineRevisionAssessor).assess(request())
        ordered.verify(purchaseDeliveryLineReplacer).replace(validatedRevision)
        ordered.verify(stockRevisionCommitter).commitReset(stockEntryResetPlan, quantityBreakdown)
        ordered.verify(stockReplacementLayerRecorder).record(
            any(StockReplacementLayerRequest::class.java) ?: layerRequestPlaceholder()
        )
        ordered.verify(stockRevisionCommitter).commitReplay(
            consumerReplayPlans,
            replacementStockEntryId,
            quantityBreakdown
        )
        ordered.verify(stockRevisionRepository).save(any(StockRevisionEntity::class.java) ?: unusedRevisionEntity())
        ordered.verify(purchaseDeliveryRevisionApplier).apply(
            purchaseId,
            replacementLine.purchaseLineId,
            deliveredStorageQuantity
        )
        assertEquals(purchaseResponse, response)
    }

    @Test
    fun `the revision is published for the ledger with both line totals after the stock is written`() {
        stubCollaborators()

        deliveryLineRevisionInitiator.initiateDeliveryLineRevision(request())

        val revisedLineTotal = BigDecimal("100.00")
        val replacementLineTotal = BigDecimal("26.00")
        verify(stockRevisionHandlerForKafka).publish(
            any(StockRevisionEntity::class.java) ?: unusedRevisionEntity(),
            eq(purchaseId) ?: purchaseId,
            eq(revisedLineTotal) ?: revisedLineTotal,
            eq(replacementLineTotal) ?: replacementLineTotal
        )
    }

    @Test
    fun `the replacement layer keeps the delivery document, takes the new line as detail and costs the whole batch`() {
        stubCollaborators()

        deliveryLineRevisionInitiator.initiateDeliveryLineRevision(request())

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
    fun `a revision with a blocking problem is refused under the locks before anything is written`() {
        stubCollaborators()
        val problem = BlockingProblem("SAL000420 (draft)", "Draft sale holding 4 pcs of Sugar.", "Remove it.")
        `when`(deliveryLineRevisionAssessor.assess(request())).thenReturn(
            DeliveryLineRevisionAssessment(
                validatedRevision, quantityBreakdown, ReplayResult(emptyList(), emptyList()), listOf(problem)
            )
        )

        val exception = assertThrows(BlockedChangeException::class.java) {
            deliveryLineRevisionInitiator.initiateDeliveryLineRevision(request())
        }

        assertEquals(listOf(problem), exception.problems)
        verify(entityAdvisoryLock).acquire(LockNamespaces.PRODUCT, locationProductId)
        verifyNoInteractions(
            purchaseDeliveryLineReplacer, stockRevisionCommitter, stockReplacementLayerRecorder,
            stockRevisionRepository, purchaseDeliveryRevisionApplier
        )
    }

    @Test
    fun `the stock revision links the revised and replacement layers under the drawn reference`() {
        stubCollaborators()

        deliveryLineRevisionInitiator.initiateDeliveryLineRevision(request())

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

        deliveryLineRevisionInitiator.initiateDeliveryLineRevision(request())

        val layerRequestCaptor = ArgumentCaptor.forClass(StockReplacementLayerRequest::class.java)
        verify(stockReplacementLayerRecorder).record(layerRequestCaptor.capture() ?: layerRequestPlaceholder())
        assertEquals(revisedStockEntryId, layerRequestCaptor.value.revisedStockEntryId)
    }

    @Test
    fun `a layer drawn by a consumer no ledger restatement covers is refused before the stock is reset`() {
        stubCollaborators()
        val transferOut = StockEntryConsumption(
            stockEntryId = revisedStockEntryId,
            locationProductId = locationProductId,
            consumer = StockConsumer(StockMovementOrigin("TRF000001", "TRFL000001"), MovementType.TRANSFER_OUT, null),
            quantityAsEntered = QuantityAsEntered(unitId, 1L, "whole line"),
            orderOfConsumption = OrderOfConsumption(null, null),
            netConsumedStorageQuantity = ExactQuantity.ofStorage(BigDecimal.ONE),
            netConsumedCost = BigDecimal("10.00"),
        )
        `when`(netConsumptionCalculator.calculateNetConsumptions(revisedStockEntryId)).thenReturn(listOf(transferOut))

        assertThrows(IllegalStateException::class.java) {
            deliveryLineRevisionInitiator.initiateDeliveryLineRevision(request())
        }

        verifyNoInteractions(stockRevisionCommitter, stockReplacementLayerRecorder, stockRevisionRepository)
    }

    @Test
    fun `a blank reason is refused before anything is read or locked`() {
        assertThrows(RtsGenericException::class.java) {
            deliveryLineRevisionInitiator.initiateDeliveryLineRevision(request(reason = "  "))
        }

        verifyNoInteractions(purchaseDeliveryDataFetcher, entityAdvisoryLock, stockRevisionCommitter)
    }

    private fun unusedRevisionEntity() = StockRevisionEntity(
        StockRevisionSourceType.DELIVERY_LINE,
        "",
        UUID.randomUUID(),
        UUID.randomUUID(),
        ""
    )
}
