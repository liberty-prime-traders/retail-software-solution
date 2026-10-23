package me.ezra_home.retail_software_solution.locations.business.supplier_payment

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID
import me.ezra_home.retail_software_solution.configuration.session.LocationSession
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryDataFetcher
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDataFetcher
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchasePaymentCeilingService
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchasePaymentCeilingService.PaymentCeiling
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseUpdater
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.api.PurchasePaymentStatusService
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.api.SupplierPaymentService
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.api.SupplierPaymentVoidCreateDto
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.organizations.business.payment_method.api.PaymentMethodService
import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class SupplierPaymentVoidStatusTest {

    private val purchaseId = UUID.randomUUID()

    @BeforeEach
    fun setSession() {
        SessionContextProvider.setSession(
            SessionContext(
                organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"),
                location = LocationSession(id = UUID.randomUUID(), schemaName = "loc-1")
            )
        )
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    private fun voidPaymentAndCaptureStatus(paymentAmount: String, preVoidPaidTotal: String): PaymentStatus {
        val supplierPaymentRepository = mock(SupplierPaymentRepository::class.java)
        val supplierPaymentVoidMapper = mock(SupplierPaymentVoidMapper::class.java)
        val purchaseUpdater = mock(PurchaseUpdater::class.java)
        val purchasePaymentCeilingService = mock(PurchasePaymentCeilingService::class.java)
        val paymentsCalculatorService = mock(PaymentsCalculatorService::class.java)

        val payment = SupplierPaymentEntity(
            purchaseId = purchaseId,
            paymentMethodId = UUID.randomUUID(),
            paymentMethodAccountCode = "CASH",
            amount = BigDecimal(paymentAmount),
            paymentDate = OffsetDateTime.now()
        ).also {
            it.id = UUID.randomUUID()
            it.referenceNumber = "SUP000001"
        }
        val voidCreateDto = SupplierPaymentVoidCreateDto(payment.id!!, "entered twice")
        `when`(supplierPaymentRepository.findById(payment.id!!)).thenReturn(Optional.of(payment))
        `when`(supplierPaymentVoidMapper.toEntity(voidCreateDto)).thenReturn(SupplierPaymentVoidEntity(payment.id!!, "entered twice"))
        `when`(paymentsCalculatorService.calculatePaidAmountForPurchase(purchaseId)).thenReturn(BigDecimal(preVoidPaidTotal))
        `when`(purchasePaymentCeilingService.computeCeiling(purchaseId))
            .thenReturn(PaymentCeiling(BigDecimal("100"), isFullyDelivered = true, deliveredTotal = BigDecimal("100")))

        val supplierPaymentService = SupplierPaymentService(
            supplierPaymentRepository,
            mock(SupplierPaymentVoidRepository::class.java),
            mock(SupplierPaymentMapper::class.java),
            supplierPaymentVoidMapper,
            mock(PurchaseDataFetcher::class.java),
            purchaseUpdater,
            mock(PaymentMethodService::class.java),
            mock(SupplierPaymentHandlerForKafka::class.java),
            mock(SupplierPaymentVoidHandlerForKafka::class.java),
            mock(SupplierPaymentAssembler::class.java),
            purchasePaymentCeilingService,
            PurchasePaymentStatusService(purchasePaymentCeilingService, purchaseUpdater, paymentsCalculatorService),
            paymentsCalculatorService,
            mock(PurchaseDeliveryDataFetcher::class.java),
            mock(FiscalPeriodService::class.java)
        )

        supplierPaymentService.voidPayment(voidCreateDto)

        return PaymentStatus.entries.first { status ->
            runCatching { verify(purchaseUpdater).updatePaymentStatus(purchaseId, status) }.isSuccess
        }
    }

    @Test
    fun `voiding the only payment resolves UNPAID`() {
        kotlin.test.assertEquals(PaymentStatus.UNPAID, voidPaymentAndCaptureStatus(paymentAmount = "100", preVoidPaidTotal = "100"))
    }

    @Test
    fun `voiding one of two payments resolves PARTIALLY_SETTLED`() {
        kotlin.test.assertEquals(PaymentStatus.PARTIALLY_SETTLED, voidPaymentAndCaptureStatus(paymentAmount = "40", preVoidPaidTotal = "100"))
    }
}
