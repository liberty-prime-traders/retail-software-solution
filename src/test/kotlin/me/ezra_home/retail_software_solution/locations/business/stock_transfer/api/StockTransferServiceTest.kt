package me.ezra_home.retail_software_solution.locations.business.stock_transfer.api

import me.ezra_home.retail_software_solution.configuration.session.LocationSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferCancelledHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDispatchRepository
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferOrderLocker
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferResponseAssembler
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferSchemaGateway
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationService
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderDataFetcher
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderDomainDto
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderService
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferStatus
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class StockTransferServiceTest {

    private val orderId = UUID.randomUUID()
    private val sourceLocationId = UUID.randomUUID()
    private val orderReferenceNumber = "ORD-1"
    private val sourceSchema = "loc-source"

    private val stockTransferOrderDataFetcher = mock(StockTransferOrderDataFetcher::class.java)
    private val stockTransferOrderService = mock(StockTransferOrderService::class.java)
    private val stockTransferSchemaGateway = mock(StockTransferSchemaGateway::class.java)
    private val stockTransferCancelledHandlerForKafka = mock(StockTransferCancelledHandlerForKafka::class.java)
    private val entityAdvisoryLock = mock(EntityAdvisoryLock::class.java)
    private val stockTransferOrderLocker = StockTransferOrderLocker(stockTransferOrderDataFetcher, entityAdvisoryLock)
    private val locationService = mock(LocationService::class.java)
    private val stockTransferService = StockTransferService(
        stockTransferOrderService = stockTransferOrderService,
        stockTransferOrderDataFetcher = stockTransferOrderDataFetcher,
        stockTransferDispatchRepository = mock(StockTransferDispatchRepository::class.java),
        stockTransferSchemaGateway = stockTransferSchemaGateway,
        stockTransferResponseAssembler = mock(StockTransferResponseAssembler::class.java),
        stockTransferCancelledHandlerForKafka = stockTransferCancelledHandlerForKafka,
        stockTransferOrderLocker = stockTransferOrderLocker,
        locationService = locationService
    )

    @BeforeEach
    fun setUpSourceSession() {
        SessionContextProvider.setSession(
            SessionContext(location = LocationSession(UUID.randomUUID(), sourceSchema, "UTC"))
        )
        `when`(locationService.getSchemaByLocationId(sourceLocationId)).thenReturn(sourceSchema)
        `when`(stockTransferSchemaGateway.cancelDispatch(orderReferenceNumber)).thenReturn("DISPATCH-1")
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    private fun order(status: StockTransferStatus, totalDispatchedCost: BigDecimal? = BigDecimal("10.0000")) =
        StockTransferOrderDomainDto(
            id = orderId,
            referenceNumber = orderReferenceNumber,
            sourceLocationId = sourceLocationId,
            destinationLocationId = UUID.randomUUID(),
            status = status,
            notes = null,
            lineCount = 1,
            totalDispatchedCost = totalDispatchedCost,
            dispatchedAt = OffsetDateTime.now(),
            dispatchedByName = null,
            createdById = UUID.randomUUID(),
            createdOn = OffsetDateTime.now()
        )

    @Test
    fun `cancelling a dispatched transfer cancels the dispatch and publishes the cancelled event with its cost`() {
        `when`(stockTransferOrderDataFetcher.getByReferenceNumber(orderReferenceNumber))
            .thenReturn(order(StockTransferStatus.DISPATCHED))

        stockTransferService.cancelTransfer(orderReferenceNumber)

        val inOrder = inOrder(
            entityAdvisoryLock, stockTransferSchemaGateway, stockTransferOrderService,
            stockTransferCancelledHandlerForKafka
        )
        inOrder.verify(entityAdvisoryLock).acquire(LockNamespaces.STOCK_TRANSFER_ORDER, orderId)
        inOrder.verify(stockTransferSchemaGateway).cancelDispatch(orderReferenceNumber)
        inOrder.verify(stockTransferOrderService).updateStatusToCancelled(orderReferenceNumber)
        inOrder.verify(stockTransferCancelledHandlerForKafka).publish(
            orderId, orderReferenceNumber, "DISPATCH-1", sourceSchema, Money.of(BigDecimal("10.0000"))
        )
    }

    @Test
    fun `cancelling a draft transfer publishes nothing, because no stock or ledger moved`() {
        `when`(stockTransferOrderDataFetcher.getByReferenceNumber(orderReferenceNumber))
            .thenReturn(order(StockTransferStatus.DRAFT, totalDispatchedCost = null))

        stockTransferService.cancelTransfer(orderReferenceNumber)

        verify(stockTransferOrderService).updateStatusToCancelled(orderReferenceNumber)
        verifyNoInteractions(stockTransferCancelledHandlerForKafka)
    }

    @Test
    fun `a transfer that is already completed or cancelled cannot be cancelled`() {
        `when`(stockTransferOrderDataFetcher.getByReferenceNumber(orderReferenceNumber))
            .thenReturn(order(StockTransferStatus.CANCELLED))

        assertThrows(RtsGenericException::class.java) { stockTransferService.cancelTransfer(orderReferenceNumber) }

        verifyNoInteractions(stockTransferCancelledHandlerForKafka, stockTransferOrderService)
    }

    @Test
    fun `cancelling a transfer that was completed while waiting for the order lock throws and restores nothing`() {
        `when`(stockTransferOrderDataFetcher.getByReferenceNumber(orderReferenceNumber))
            .thenReturn(order(StockTransferStatus.DISPATCHED), order(StockTransferStatus.COMPLETED))

        assertThrows(RtsGenericException::class.java) { stockTransferService.cancelTransfer(orderReferenceNumber) }

        verifyNoInteractions(stockTransferSchemaGateway, stockTransferCancelledHandlerForKafka, stockTransferOrderService)
    }

    @Test
    fun `the order status is read again after the order lock is taken`() {
        `when`(stockTransferOrderDataFetcher.getByReferenceNumber(orderReferenceNumber))
            .thenReturn(order(StockTransferStatus.DISPATCHED), order(StockTransferStatus.COMPLETED))

        assertThrows(RtsGenericException::class.java) { stockTransferService.cancelTransfer(orderReferenceNumber) }

        val inOrder = inOrder(stockTransferOrderDataFetcher, entityAdvisoryLock)
        inOrder.verify(stockTransferOrderDataFetcher).getByReferenceNumber(orderReferenceNumber)
        inOrder.verify(entityAdvisoryLock).acquire(LockNamespaces.STOCK_TRANSFER_ORDER, orderId)
        inOrder.verify(stockTransferOrderDataFetcher).getByReferenceNumber(orderReferenceNumber)
    }
}
