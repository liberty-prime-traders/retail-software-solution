package me.ezra_home.retail_software_solution.locations.business.supplier_payment

import me.ezra_home.retail_software_solution.configuration.session.LocationSession
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryDataFetcher
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDataFetcher
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDataFetcher.PurchaseInfo
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseStatus
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseUpdater
import me.ezra_home.retail_software_solution.locations.business.purchase.api.payment.PurchasePaymentCeilingService
import me.ezra_home.retail_software_solution.locations.business.purchase.api.payment.PurchasePaymentCeilingService.PaymentCeiling
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.api.PurchasePaymentStatusService
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.api.SupplierPaymentCreateDto
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.api.SupplierPaymentResponseDto
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.api.SupplierPaymentService
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.organizations.business.payment_method.api.PaymentMethodDataFetcher
import me.ezra_home.retail_software_solution.util.business.PositiveMoney
import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class SupplierPaymentBatchRecordingTest {

    private val paymentMethodId = UUID.randomUUID()
    private val supplierId = UUID.randomUUID()
    private val paymentDate = OffsetDateTime.of(2026, 3, 1, 10, 0, 0, 0, ZoneOffset.UTC)

    private val supplierPaymentRepository = mock(SupplierPaymentRepository::class.java)
    private val supplierPaymentMapper = mock(SupplierPaymentMapper::class.java)
    private val purchaseDataFetcher = mock(PurchaseDataFetcher::class.java)
    private val purchaseUpdater = mock(PurchaseUpdater::class.java)
    private val paymentMethodDataFetcher = mock(PaymentMethodDataFetcher::class.java)
    private val supplierPaymentHandlerForKafka = mock(SupplierPaymentHandlerForKafka::class.java)
    private val assembler = mock(SupplierPaymentAssembler::class.java)
    private val purchasePaymentCeilingService = mock(PurchasePaymentCeilingService::class.java)
    private val paymentsCalculatorService = mock(PaymentsCalculatorService::class.java)
    private val responsePurchaseInfos = mutableListOf<Map<UUID, PurchaseInfo>>()

    private val supplierPaymentService = SupplierPaymentService(
        supplierPaymentRepository,
        mock(SupplierPaymentVoidRepository::class.java),
        supplierPaymentMapper,
        mock(SupplierPaymentVoidMapper::class.java),
        purchaseDataFetcher,
        purchaseUpdater,
        paymentMethodDataFetcher,
        supplierPaymentHandlerForKafka,
        mock(SupplierPaymentVoidHandlerForKafka::class.java),
        assembler,
        purchasePaymentCeilingService,
        PurchasePaymentStatusService(purchasePaymentCeilingService, purchaseUpdater, paymentsCalculatorService),
        paymentsCalculatorService,
        mock(PurchaseDeliveryDataFetcher::class.java),
        mock(FiscalPeriodService::class.java)
    )

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNonNull(): T {
        ArgumentMatchers.any<T>()
        return null as T
    }

    @BeforeEach
    fun setUp() {
        SessionContextProvider.setSession(
            SessionContext(
                organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"),
                location = LocationSession(id = UUID.randomUUID(), schemaName = "loc-1")
            )
        )
        `when`(paymentMethodDataFetcher.findAccountCodesByIds(setOf(paymentMethodId)))
            .thenReturn(mapOf(paymentMethodId to "CASH"))
        doAnswer { invocation ->
            val createDto = invocation.getArgument<SupplierPaymentCreateDto>(0)
            SupplierPaymentEntity(
                purchaseId = createDto.purchaseId,
                paymentMethodId = createDto.paymentMethodId,
                paymentMethodAccountCode = invocation.getArgument(1),
                amount = createDto.amount.toBigDecimal(),
                paymentDate = createDto.paymentDate,
                notes = createDto.notes
            ).also { it.id = UUID.randomUUID() }
        }.`when`(supplierPaymentMapper).toEntity(anyNonNull(), anyNonNull())
        doAnswer { invocation ->
            responsePurchaseInfos.add(invocation.getArgument(2))
            invocation.getArgument<List<SupplierPaymentEntity>>(0).map { entity ->
                SupplierPaymentResponseDto(
                    id = entity.id!!, referenceNumber = "SUP", purchaseId = entity.purchaseId,
                    purchaseReferenceNumber = "", paymentMethod = "", supplier = "", amount = entity.amount,
                    paymentDate = entity.paymentDate, notes = null, createdBy = "", createdOn = ""
                )
            }
        }.`when`(assembler).buildResponses(anyNonNull(), anyNonNull(), anyNonNull<Map<UUID, PurchaseInfo>>())
        doAnswer { invocation ->
            invocation.getArgument<Set<UUID>>(0).associateWith { purchaseId ->
                PurchaseInfo(purchaseId, "PRCH", supplierId, PurchaseStatus.ORDERED, PaymentStatus.UNPAID)
            }
        }.`when`(purchaseDataFetcher).findPurchaseInfoByIds(anyNonNull())
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    private fun createDto(purchaseId: UUID, amount: String) = SupplierPaymentCreateDto(
        purchaseId = purchaseId,
        deliveryId = null,
        paymentMethodId = paymentMethodId,
        amount = PositiveMoney.of(BigDecimal(amount)),
        paymentDate = paymentDate,
        notes = null
    )

    private fun purchaseInfos(vararg purchaseIds: UUID) = purchaseIds.associateWith { purchaseId ->
        PurchaseInfo(purchaseId, "PRCH", supplierId, PurchaseStatus.ORDERED, PaymentStatus.UNPAID)
    }

    private fun ceiling(amount: String, isFullyDelivered: Boolean) =
        PaymentCeiling(BigDecimal(amount), isFullyDelivered, BigDecimal(amount), BigDecimal(amount))

    @Test
    fun `records every payment, updates each purchase status and publishes each event`() {
        val first = UUID.randomUUID()
        val second = UUID.randomUUID()

        val responses = supplierPaymentService.recordPaymentsForLockedPurchases(
            listOf(createDto(first, "100.00"), createDto(second, "30.00")),
            mapOf(first to ceiling("100.00", true), second to ceiling("100.00", true)),
            mapOf(first to BigDecimal.ZERO, second to BigDecimal("20.00")),
            purchaseInfos(first, second)
        )

        assertEquals(2, responses.size)
        verify(purchaseUpdater).updatePaymentStatuses(
            mapOf(first to PaymentStatus.FULLY_SETTLED, second to PaymentStatus.PARTIALLY_SETTLED)
        )
        verify(supplierPaymentHandlerForKafka, times(2)).publish(anyNonNull(), anyNonNull())
    }

    @Test
    fun `the purchases handed in are not read again and the response carries the stored status`() {
        val purchaseId = UUID.randomUUID()

        supplierPaymentService.recordPaymentsForLockedPurchases(
            listOf(createDto(purchaseId, "100.00")),
            mapOf(purchaseId to ceiling("100.00", true)),
            mapOf(purchaseId to BigDecimal.ZERO),
            purchaseInfos(purchaseId)
        )

        verify(purchaseDataFetcher, never()).findPurchaseInfoByIds(anyNonNull())
        assertEquals(PaymentStatus.FULLY_SETTLED, responsePurchaseInfos.single().getValue(purchaseId).paymentStatus)
    }

    @Test
    fun `recordPayment locks the purchase before reading its balances`() {
        val purchaseId = UUID.randomUUID()
        `when`(purchasePaymentCeilingService.computeCeiling(purchaseId)).thenReturn(ceiling("100.00", false))
        `when`(paymentsCalculatorService.calculatePaidAmountForPurchase(purchaseId)).thenReturn(BigDecimal.ZERO)

        supplierPaymentService.recordPayment(createDto(purchaseId, "10.00"))

        val lockThenRead = inOrder(purchaseDataFetcher, purchasePaymentCeilingService, paymentsCalculatorService)
        lockThenRead.verify(purchaseDataFetcher).lockPurchase(purchaseId)
        lockThenRead.verify(purchasePaymentCeilingService).computeCeiling(purchaseId)
        lockThenRead.verify(paymentsCalculatorService).calculatePaidAmountForPurchase(purchaseId)
    }

    @Test
    fun `rejects two payments for the same purchase`() {
        val purchaseId = UUID.randomUUID()

        assertThrows<RtsGenericException> {
            supplierPaymentService.recordPaymentsForLockedPurchases(
                listOf(createDto(purchaseId, "10.00"), createDto(purchaseId, "10.00")),
                mapOf(purchaseId to ceiling("100.00", true)),
                mapOf(purchaseId to BigDecimal.ZERO),
                purchaseInfos(purchaseId)
            )
        }
        verify(supplierPaymentRepository, never()).saveAll(anyNonNull<List<SupplierPaymentEntity>>())
    }

    @Test
    fun `rejects a payment above the delivered balance of a fully delivered purchase`() {
        val purchaseId = UUID.randomUUID()

        assertThrows<RtsGenericException> {
            supplierPaymentService.recordPaymentsForLockedPurchases(
                listOf(createDto(purchaseId, "60.00")),
                mapOf(purchaseId to ceiling("100.00", true)),
                mapOf(purchaseId to BigDecimal("50.00")),
                purchaseInfos(purchaseId)
            )
        }
        verify(supplierPaymentRepository, never()).saveAll(anyNonNull<List<SupplierPaymentEntity>>())
    }
}
