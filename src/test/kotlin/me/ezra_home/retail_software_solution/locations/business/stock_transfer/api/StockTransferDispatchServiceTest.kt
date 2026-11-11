package me.ezra_home.retail_software_solution.locations.business.stock_transfer.api

import me.ezra_home.retail_software_solution.configuration.session.LocationSession
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductStockBreakdownProvider
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.stock.api.availability.StockAvailabilityValidator
import me.ezra_home.retail_software_solution.locations.business.stock.api.fifo.FifoLayer
import me.ezra_home.retail_software_solution.locations.business.stock.api.fifo.StockEntryFifoDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.read.StockBalanceFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.updater.StockTransferDispatchLineStockRequest
import me.ezra_home.retail_software_solution.locations.business.stock.api.updater.StockTransferStockUpdater
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDispatchEntity
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDispatchLineEntity
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDispatchLineRepository
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDispatchRepository
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDispatchedHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDraftDispatchFetcher
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDraftLineEntity
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDraftLineRepository
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferOrderLocker
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferResponseAssembler
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferSchemaGateway
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationService
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderDataFetcher
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderDomainDto
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderService
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferStatus
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraph
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.BreakdownUnit
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.ArgumentMatchers.anyIterable
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.invocation.Invocation
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class StockTransferDispatchServiceTest {

    private val orderId = UUID.randomUUID()
    private val destinationLocationId = UUID.randomUUID()
    private val userId = UUID.randomUUID()
    private val orderReferenceNumber = "ORD-1"

    private val stockTransferOrderService = mock(StockTransferOrderService::class.java)
    private val stockTransferDispatchRepository = mock(StockTransferDispatchRepository::class.java)
    private val stockTransferDraftLineRepository = mock(StockTransferDraftLineRepository::class.java)
    private val stockTransferDispatchLineRepository = mock(StockTransferDispatchLineRepository::class.java)
    private val stockBalanceFetcher = mock(StockBalanceFetcher::class.java)
    private val stockTransferDispatchedHandlerForKafka = mock(StockTransferDispatchedHandlerForKafka::class.java)
    private val stockTransferStockUpdater = mock(StockTransferStockUpdater::class.java)
    private val stockTransferResponseAssembler = mock(StockTransferResponseAssembler::class.java)
    private val locationProductDataFetcher = mock(LocationProductDataFetcher::class.java)
    private val locationService = mock(LocationService::class.java)
    private val userQualifier = mock(UserQualifier::class.java)
    private val stockTransferDraftDispatchFetcher = mock(StockTransferDraftDispatchFetcher::class.java)
    private val entityAdvisoryLock = mock(EntityAdvisoryLock::class.java)
    private val stockAvailabilityValidator = mock(StockAvailabilityValidator::class.java)
    private val productStockBreakdownProvider = mock(ProductStockBreakdownProvider::class.java)
    private val stockTransferOrderDataFetcher = mock(StockTransferOrderDataFetcher::class.java)
    private val stockTransferOrderLocker = StockTransferOrderLocker(stockTransferOrderDataFetcher, entityAdvisoryLock)
    private val stockTransferSchemaGateway = mock(StockTransferSchemaGateway::class.java)
    private val unitConversionGraphFacade = mock(UnitConversionGraphFacade::class.java)
    private val stockTransferDispatchService = StockTransferDispatchService(
        stockTransferOrderService = stockTransferOrderService,
        stockTransferDispatchRepository = stockTransferDispatchRepository,
        stockTransferDraftLineRepository = stockTransferDraftLineRepository,
        stockTransferDispatchLineRepository = stockTransferDispatchLineRepository,
        stockBalanceFetcher = stockBalanceFetcher,
        stockTransferDispatchedHandlerForKafka = stockTransferDispatchedHandlerForKafka,
        stockTransferStockUpdater = stockTransferStockUpdater,
        stockTransferResponseAssembler = stockTransferResponseAssembler,
        locationProductDataFetcher = locationProductDataFetcher,
        locationService = locationService,
        userQualifier = userQualifier,
        stockTransferDraftDispatchFetcher = stockTransferDraftDispatchFetcher,
        entityAdvisoryLock = entityAdvisoryLock,
        stockAvailabilityValidator = stockAvailabilityValidator,
        productStockBreakdownProvider = productStockBreakdownProvider,
        stockTransferOrderLocker = stockTransferOrderLocker,
        stockTransferSchemaGateway = stockTransferSchemaGateway,
        unitConversionGraphFacade = unitConversionGraphFacade
    )

    private fun order(status: StockTransferStatus) = StockTransferOrderDomainDto(
        id = orderId,
        referenceNumber = orderReferenceNumber,
        sourceLocationId = UUID.randomUUID(),
        destinationLocationId = destinationLocationId,
        status = status,
        notes = null,
        lineCount = null,
        totalDispatchedCost = BigDecimal.ZERO,
        dispatchedAt = null,
        dispatchedByName = null,
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now()
    )

    @BeforeEach
    fun setUpSourceSession() {
        SessionContextProvider.setSession(
            SessionContext(
                systemUserId = userId,
                organization = OrgSession(UUID.randomUUID(), "org-a", "UTC"),
                location = LocationSession(UUID.randomUUID(), "loc-source", "UTC")
            )
        )
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    private fun calls(vararg collaborators: Any, named: Set<String>): List<Invocation> =
        collaborators.flatMap { mockingDetails(it).invocations }
            .filter { it.method.name in named }
            .sortedBy { it.sequenceNumber }

    @Test
    fun `dispatching a draft transfer allocates FIFO lines, drains the source and publishes the event`() {
        val dispatchId = UUID.randomUUID()
        val locationProductId = UUID.randomUUID()
        val orgProductId = UUID.randomUUID()
        val pieceId = UUID.randomUUID()
        val storageRootId = UUID.randomUUID()
        val stockEntryId = UUID.randomUUID()
        val dispatchEntity = StockTransferDispatchEntity(orderReferenceNumber)
            .also { it.id = dispatchId; it.referenceNumber = "DISPATCH-1" }
        `when`(stockTransferOrderDataFetcher.getByReferenceNumber(orderReferenceNumber))
            .thenReturn(order(StockTransferStatus.DRAFT))
        `when`(stockTransferDraftDispatchFetcher.requireDraftDispatch(orderReferenceNumber)).thenReturn(dispatchEntity)
        `when`(stockTransferDraftLineRepository.findByStockTransferDispatchId(dispatchId)).thenReturn(
            listOf(
                StockTransferDraftLineEntity(
                    stockTransferDispatchId = dispatchId,
                    locationProductId = locationProductId,
                    userProvidedQuantity = BigDecimal("30.0000"),
                    unitId = pieceId,
                    storageUnitsPerUserProvidedUnit = 1L
                )
            )
        )
        `when`(stockBalanceFetcher.getFifoEntriesByProduct(listOf(locationProductId))).thenReturn(
            mapOf(
                locationProductId to listOf(
                    StockEntryFifoDto(
                        "SE-1", locationProductId, BigDecimal("5.0000"),
                        FifoLayer(
                            stockEntryId, BigDecimal("100.0000"), BigDecimal("500.0000"),
                            BigDecimal("100.0000"), BigDecimal("500.0000")
                        )
                    )
                )
            )
        )
        `when`(locationProductDataFetcher.getOrgProductIds(listOf(locationProductId)))
            .thenReturn(mapOf(locationProductId to orgProductId))
        `when`(locationProductDataFetcher.findSummaryByIds(listOf(locationProductId))).thenReturn(
            mapOf(
                locationProductId to LocationProductSummaryDto(
                    locationProductId, "P-1", orgProductId, "Rice", "Grain", pieceId
                )
            )
        )
        `when`(productStockBreakdownProvider.forLocationProducts(anyCollection())).thenReturn(
            mapOf(
                locationProductId to QuantityBreakdown.forBaseUnit(
                    listOf(BreakdownUnit(pieceId, "Piece", 1L, pieceId, OffsetDateTime.now())), pieceId
                )
            )
        )
        `when`(locationService.getSchemaByLocationId(destinationLocationId)).thenReturn("loc-destination")
        `when`(stockTransferSchemaGateway.readBaseUnitIdsByOrgProductId(anyCollection()))
            .thenReturn(mapOf(orgProductId to pieceId))
        val unitConversionGraph = mock(UnitConversionGraph::class.java)
        `when`(unitConversionGraphFacade.getOrLoad()).thenReturn(unitConversionGraph)
        `when`(unitConversionGraph.getStorageUnitId(pieceId)).thenReturn(storageRootId)
        `when`(unitConversionGraph.getSizeInRoot(pieceId)).thenReturn(1L)
        doAnswer { invocation ->
            invocation.getArgument<Iterable<StockTransferDispatchLineEntity>>(0)
                .forEachIndexed { index, line -> line.referenceNumber = "DL-${index + 1}" }
            invocation.getArgument<Any>(0)
        }.`when`(stockTransferDispatchLineRepository).saveAll(anyIterable<StockTransferDispatchLineEntity>())
        `when`(userQualifier.getUserFullName(userId)).thenReturn("Alice")
        `when`(stockTransferOrderService.updateStatusToDispatched(orderReferenceNumber))
            .thenReturn(order(StockTransferStatus.DISPATCHED))

        stockTransferDispatchService.dispatch(orderReferenceNumber)

        val writes = calls(
            entityAdvisoryLock, stockTransferDraftLineRepository, stockTransferDispatchLineRepository,
            stockTransferStockUpdater, stockTransferOrderService, stockTransferDispatchedHandlerForKafka,
            named = setOf(
                "acquire", "deleteByStockTransferDispatchId", "saveAll", "consumeStockForDispatch",
                "setDispatchSummary", "updateStatusToDispatched", "publish"
            )
        )
        assertEquals(
            listOf(
                "acquire", "acquire", "deleteByStockTransferDispatchId", "saveAll", "consumeStockForDispatch",
                "setDispatchSummary", "updateStatusToDispatched", "publish"
            ),
            writes.map { it.method.name }
        )
        assertEquals(LockNamespaces.STOCK_TRANSFER_ORDER, writes[0].arguments[0])
        assertEquals(LockNamespaces.PRODUCT, writes[1].arguments[0])

        val dispatchLine = (writes[3].arguments[0] as List<*>).single() as StockTransferDispatchLineEntity
        assertEquals(orgProductId, dispatchLine.orgProductId)
        assertSameNumber("30", dispatchLine.storageQuantityDispatched)
        assertSameNumber("150", dispatchLine.costDispatched)

        val consumeRequest = (writes[4].arguments[0] as List<*>).single() as StockTransferDispatchLineStockRequest
        assertEquals("DISPATCH-1", consumeRequest.dispatchReference)
        assertEquals("DL-1", consumeRequest.dispatchLineReference)
        assertEquals(locationProductId, consumeRequest.locationProductId)

        val summary = writes[5].arguments
        assertEquals(orderReferenceNumber, summary[0])
        assertEquals(1, summary[1])
        assertSameNumber("150", summary[2] as BigDecimal)
        val dispatchedAt = summary[3] as OffsetDateTime
        assertEquals("Alice", summary[4])

        assertEquals(StockTransferStatus.DISPATCHED, dispatchEntity.status)
        assertEquals(dispatchedAt, dispatchEntity.dispatchedAt)
        assertEquals(userId, dispatchEntity.dispatchedById)

        val published = writes[7].arguments
        assertEquals(orderId, published[0])
        assertEquals("DISPATCH-1", published[2])
        assertEquals("loc-source", published[3])
        assertEquals(Money.of(BigDecimal("150.0000")), published[4])
        assertEquals(DateTimes.Local.atOrganizationZone(dispatchedAt), published[5])
    }

    private fun assertSameNumber(expected: String, actual: BigDecimal) =
        assertEquals(0, BigDecimal(expected).compareTo(actual), "expected $expected but was $actual")

    @Test
    fun `dispatch takes the order lock before it loads the draft dispatch, so a cancel that won the lock is seen`() {
        `when`(stockTransferOrderDataFetcher.getByReferenceNumber(orderReferenceNumber))
            .thenReturn(order(StockTransferStatus.DRAFT))
        `when`(stockTransferDraftDispatchFetcher.requireDraftDispatch(orderReferenceNumber))
            .thenThrow(RtsGenericException("Operation only allowed in DRAFT status. Current: CANCELLED"))

        assertThrows(RtsGenericException::class.java) { stockTransferDispatchService.dispatch(orderReferenceNumber) }

        val inOrder = inOrder(entityAdvisoryLock, stockTransferDraftDispatchFetcher)
        inOrder.verify(entityAdvisoryLock).acquire(LockNamespaces.STOCK_TRANSFER_ORDER, orderId)
        inOrder.verify(stockTransferDraftDispatchFetcher).requireDraftDispatch(orderReferenceNumber)
        verifyNoInteractions(stockTransferStockUpdater, stockTransferDispatchedHandlerForKafka)
    }
}
