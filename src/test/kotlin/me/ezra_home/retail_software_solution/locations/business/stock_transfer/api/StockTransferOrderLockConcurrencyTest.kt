package me.ezra_home.retail_software_solution.locations.business.stock_transfer.api

import me.ezra_home.retail_software_solution.configuration.session.LocationSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.stock.api.updater.StockTransferReceiptStockUpdater
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.DispatchWithLines
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferCancelledHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDispatchEntity
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDispatchLineEntity
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDispatchRepository
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferReceiptCompletedHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferReceiptEntity
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferReceiptLineEntity
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferReceiptLineRepository
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferReceiptRepository
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferReceiptStatus
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferOrderLocker
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferResponseAssembler
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferSchemaGateway
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationService
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderDataFetcher
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderDomainDto
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderService
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferStatus
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock

// A ReentrantLock stands in for the advisory lock and each thread releases it when its "transaction" ends, so these tests
// prove the services' use of the lock (take it first, re-read the status after it), not the database lock itself.
@Timeout(30)
class StockTransferOrderLockConcurrencyTest {

    private val orderId = UUID.randomUUID()
    private val sourceLocationId = UUID.randomUUID()
    private val receiptId = UUID.randomUUID()
    private val orderReferenceNumber = "ORD-1"
    private val receiptReferenceNumber = "RCP-1"
    private val dispatchReferenceNumber = "DISPATCH-1"
    private val sourceSchema = "loc-source"

    private val orderStatus = AtomicReference(StockTransferStatus.DISPATCHED)
    private val orderLock = ReentrantLock()
    private val cancelIsWaitingForLock = CountDownLatch(1)
    private val completeIsWaitingForLock = CountDownLatch(1)

    private val stockTransferOrderDataFetcher = mock(StockTransferOrderDataFetcher::class.java)
    private val stockTransferOrderService = mock(StockTransferOrderService::class.java)
    private val stockTransferSchemaGateway = mock(StockTransferSchemaGateway::class.java)
    private val stockTransferReceiptRepository = mock(StockTransferReceiptRepository::class.java)
    private val stockTransferReceiptLineRepository = mock(StockTransferReceiptLineRepository::class.java)
    private val stockTransferReceiptStockUpdater = mock(StockTransferReceiptStockUpdater::class.java)
    private val stockTransferCancelledHandlerForKafka = mock(StockTransferCancelledHandlerForKafka::class.java)
    private val stockTransferReceiptCompletedHandlerForKafka =
        mock(StockTransferReceiptCompletedHandlerForKafka::class.java)
    private val locationService = mock(LocationService::class.java)
    private val entityAdvisoryLock = mock(EntityAdvisoryLock::class.java)
    private val stockTransferOrderLocker = StockTransferOrderLocker(stockTransferOrderDataFetcher, entityAdvisoryLock)

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
    private val stockTransferReceiptService = StockTransferReceiptService(
        stockTransferOrderService = stockTransferOrderService,
        stockTransferOrderDataFetcher = stockTransferOrderDataFetcher,
        stockTransferReceiptRepository = stockTransferReceiptRepository,
        stockTransferReceiptLineRepository = stockTransferReceiptLineRepository,
        stockTransferResponseAssembler = mock(StockTransferResponseAssembler::class.java),
        stockTransferSchemaGateway = stockTransferSchemaGateway,
        locationProductDataFetcher = mock(LocationProductDataFetcher::class.java),
        stockTransferReceiptCompletedHandlerForKafka = stockTransferReceiptCompletedHandlerForKafka,
        stockTransferReceiptStockUpdater = stockTransferReceiptStockUpdater,
        stockTransferOrderLocker = stockTransferOrderLocker,
        locationService = locationService
    )

    init {
        `when`(stockTransferOrderDataFetcher.getByReferenceNumber(orderReferenceNumber))
            .thenAnswer { order(orderStatus.get()) }
        doAnswer { orderStatus.set(StockTransferStatus.COMPLETED); order(StockTransferStatus.COMPLETED) }
            .`when`(stockTransferOrderService).updateStatusToCompleted(orderReferenceNumber)
        doAnswer { orderStatus.set(StockTransferStatus.CANCELLED); order(StockTransferStatus.CANCELLED) }
            .`when`(stockTransferOrderService).updateStatusToCancelled(orderReferenceNumber)
        `when`(locationService.getSchemaByLocationId(sourceLocationId)).thenReturn(sourceSchema)
        `when`(stockTransferSchemaGateway.cancelDispatch(orderReferenceNumber)).thenReturn(dispatchReferenceNumber)
        `when`(stockTransferSchemaGateway.readDispatchAndLines(orderReferenceNumber)).thenReturn(
            DispatchWithLines(
                StockTransferDispatchEntity(orderReferenceNumber).also { it.referenceNumber = dispatchReferenceNumber },
                listOf(dispatchLine())
            )
        )
        `when`(stockTransferReceiptRepository.findStockTransferOrderRefByReferenceNumber(receiptReferenceNumber))
            .thenReturn(orderReferenceNumber)
        `when`(stockTransferReceiptRepository.findByReferenceNumber(receiptReferenceNumber)).thenReturn(
            StockTransferReceiptEntity(
                orderReferenceNumber, UUID.randomUUID(), OffsetDateTime.now(), StockTransferReceiptStatus.PENDING
            ).also { it.id = receiptId; it.referenceNumber = receiptReferenceNumber }
        )
        `when`(stockTransferReceiptLineRepository.findByStockTransferReceiptId(receiptId)).thenReturn(
            listOf(StockTransferReceiptLineEntity(receiptId, "DL-1", UUID.randomUUID(), BigDecimal("36.0000")))
        )
        doAnswer {
            if (Thread.currentThread().name == "cancel") cancelIsWaitingForLock.countDown()
            if (Thread.currentThread().name == "complete") completeIsWaitingForLock.countDown()
            orderLock.lock()
        }.`when`(entityAdvisoryLock).acquire(LockNamespaces.STOCK_TRANSFER_ORDER, orderId)
    }

    private fun order(status: StockTransferStatus) = StockTransferOrderDomainDto(
        id = orderId,
        referenceNumber = orderReferenceNumber,
        sourceLocationId = sourceLocationId,
        destinationLocationId = UUID.randomUUID(),
        status = status,
        notes = null,
        lineCount = 1,
        totalDispatchedCost = BigDecimal("10.0000"),
        dispatchedAt = OffsetDateTime.now(),
        dispatchedByName = null,
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now()
    )

    private fun dispatchLine() = StockTransferDispatchLineEntity(
        stockTransferDispatchId = UUID.randomUUID(),
        orgProductId = UUID.randomUUID(),
        locationProductId = UUID.randomUUID(),
        wholeLineUserProvidedQuantity = BigDecimal("2.0000"),
        storageQuantityDispatched = BigDecimal("36.0000"),
        unitId = UUID.randomUUID(),
        unitCost = BigDecimal("0.2777777778"),
        costDispatched = BigDecimal("10.0000"),
        storageUnitsPerUserProvidedUnit = 18L
    ).also { it.referenceNumber = "DL-1" }

    // Runs a service call as one "transaction" on its own thread: the lock taken inside it is released when it ends
    private fun inTransaction(threadName: String, call: () -> Unit): Pair<Thread, AtomicReference<Throwable?>> {
        val failure = AtomicReference<Throwable?>()
        val thread = Thread({
            SessionContextProvider.setSession(
                SessionContext(location = LocationSession(UUID.randomUUID(), "loc-$threadName", "UTC"))
            )
            try {
                call()
            } catch (throwable: Throwable) {
                failure.set(throwable)
            } finally {
                if (orderLock.isHeldByCurrentThread) orderLock.unlock()
                SessionContextProvider.clear()
            }
        }, threadName)
        thread.start()
        return thread to failure
    }

    private fun awaitBlockedOnLock(latch: CountDownLatch) {
        assertTrue(latch.await(10, TimeUnit.SECONDS), "the second operation never reached the lock")
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (!orderLock.hasQueuedThreads() && System.nanoTime() < deadline) Thread.sleep(5)
        assertTrue(orderLock.hasQueuedThreads(), "the second operation did not wait for the order lock")
    }

    @Test
    fun `a cancel arriving during a completion waits, then finds the order completed and changes nothing`() {
        val creditStarted = CountDownLatch(1)
        val releaseCredit = CountDownLatch(1)
        doAnswer {
            creditStarted.countDown()
            releaseCredit.await(10, TimeUnit.SECONDS)
        }.`when`(stockTransferReceiptStockUpdater).recordTransferReceipt(anyString(), anyList())

        val (completeThread, completeFailure) = inTransaction("complete") {
            stockTransferReceiptService.completeTransfer(receiptReferenceNumber)
        }
        assertTrue(creditStarted.await(10, TimeUnit.SECONDS))
        val (cancelThread, cancelFailure) = inTransaction("cancel") {
            stockTransferService.cancelTransfer(orderReferenceNumber)
        }
        awaitBlockedOnLock(cancelIsWaitingForLock)
        verify(stockTransferSchemaGateway, never()).cancelDispatch(orderReferenceNumber)

        releaseCredit.countDown()
        completeThread.join(10_000)
        cancelThread.join(10_000)

        assertNull(completeFailure.get())
        assertInstanceOf(RtsGenericException::class.java, cancelFailure.get())
        assertEquals(StockTransferStatus.COMPLETED, orderStatus.get())
        verify(stockTransferSchemaGateway, never()).cancelDispatch(orderReferenceNumber)
        verifyNoInteractions(stockTransferCancelledHandlerForKafka)
    }

    @Test
    fun `a completion arriving during a cancel waits, then finds the order cancelled and credits nothing`() {
        val cancelStarted = CountDownLatch(1)
        val releaseCancel = CountDownLatch(1)
        doAnswer {
            cancelStarted.countDown()
            releaseCancel.await(10, TimeUnit.SECONDS)
            dispatchReferenceNumber
        }.`when`(stockTransferSchemaGateway).cancelDispatch(orderReferenceNumber)

        val (cancelThread, cancelFailure) = inTransaction("cancel") {
            stockTransferService.cancelTransfer(orderReferenceNumber)
        }
        assertTrue(cancelStarted.await(10, TimeUnit.SECONDS))
        val (completeThread, completeFailure) = inTransaction("complete") {
            stockTransferReceiptService.completeTransfer(receiptReferenceNumber)
        }
        awaitBlockedOnLock(completeIsWaitingForLock)
        verifyNoInteractions(stockTransferReceiptStockUpdater)

        releaseCancel.countDown()
        cancelThread.join(10_000)
        completeThread.join(10_000)

        assertNull(cancelFailure.get())
        assertInstanceOf(RtsGenericException::class.java, completeFailure.get())
        assertEquals(StockTransferStatus.CANCELLED, orderStatus.get())
        verifyNoInteractions(stockTransferReceiptStockUpdater, stockTransferReceiptCompletedHandlerForKafka)
    }
}
