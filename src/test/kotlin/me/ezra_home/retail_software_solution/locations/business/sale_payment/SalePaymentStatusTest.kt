package me.ezra_home.retail_software_solution.locations.business.sale_payment

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID
import me.ezra_home.retail_software_solution.configuration.session.LocationSession
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleContext
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDataFetcher
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleUpdater
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentFetcher
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentService
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentVoidCreateDto
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.organizations.business.payment_method.api.PaymentMethodService
import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.`when`
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class SalePaymentStatusTest {

    private val saleId = UUID.randomUUID()
    private val contactId = UUID.randomUUID()
    private val paymentMethodId = UUID.randomUUID()
    private val salePaymentFetcher = mock(SalePaymentFetcher::class.java)

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

    private fun newPayment(amount: String) =
        SalePaymentWriter.NewSalePayment(paymentMethodId, BigDecimal(amount), reference = null, paymentDate = null)

    private fun salePaymentEntity(amount: String) = SalePaymentEntity(
        saleId = saleId,
        paymentMethodId = paymentMethodId,
        paymentMethodAccountCode = "CASH",
        amount = BigDecimal(amount),
        paymentDate = OffsetDateTime.now()
    ).also {
        it.id = UUID.randomUUID()
        it.referenceNumber = "SP000001"
    }

    private fun paymentMethodServiceNamingTheMethod() = mock(PaymentMethodService::class.java).also {
        `when`(it.getNamesById()).thenReturn(mapOf(paymentMethodId to "Cash"))
    }

    private fun salePaymentWriter(): SalePaymentWriter {
        val salePaymentRepository = mock(SalePaymentRepository::class.java)
        `when`(salePaymentRepository.saveAll(anyList<SalePaymentEntity>())).thenAnswer { it.getArgument<List<SalePaymentEntity>>(0) }
        val paymentMethodService = mock(PaymentMethodService::class.java)
        `when`(paymentMethodService.findAccountCode(paymentMethodId)).thenReturn("CASH")
        return SalePaymentWriter(salePaymentRepository, salePaymentFetcher, mock(SalePaymentHandlerForKafka::class.java), paymentMethodService)
    }

    @Test
    fun `write resolves FULLY_SETTLED from the pre-write paid total plus the new payment`() {
        `when`(salePaymentFetcher.calculatePaidAmount(saleId)).thenReturn(BigDecimal.ZERO)

        val result = salePaymentWriter().write(saleId, contactId, BigDecimal("100"), listOf(newPayment("100")))

        assertEquals(PaymentStatus.FULLY_SETTLED, result.newPaymentStatus)
    }

    @Test
    fun `write resolves PARTIALLY_SETTLED when earlier payments plus the new one are below the total`() {
        `when`(salePaymentFetcher.calculatePaidAmount(saleId)).thenReturn(BigDecimal("30"))

        val result = salePaymentWriter().write(saleId, contactId, BigDecimal("100"), listOf(newPayment("20"), newPayment("10")))

        assertEquals(PaymentStatus.PARTIALLY_SETTLED, result.newPaymentStatus)
    }

    @Test
    fun `write with no new payments resolves from the existing paid total`() {
        `when`(salePaymentFetcher.calculatePaidAmount(saleId)).thenReturn(BigDecimal("100"))

        val result = salePaymentWriter().write(saleId, contactId, BigDecimal("100"), emptyList())

        assertEquals(PaymentStatus.FULLY_SETTLED, result.newPaymentStatus)
    }

    @Test
    fun `voidPayment resolves UNPAID by subtracting the voided amount from the pre-void paid total`() {
        val salePaymentRepository = mock(SalePaymentRepository::class.java)
        val saleDataFetcher = mock(SaleDataFetcher::class.java)
        val saleUpdater = mock(SaleUpdater::class.java)
        val payment = salePaymentEntity("100")
        `when`(salePaymentRepository.getReferenceById(payment.id!!)).thenReturn(payment)
        `when`(saleDataFetcher.lockAndGetSaleContext(saleId)).thenReturn(SaleContext(contactId, BigDecimal("100"), SaleStatus.CONFIRMED))
        `when`(salePaymentFetcher.calculatePaidAmount(saleId)).thenReturn(BigDecimal("100"))
        val salePaymentService = SalePaymentService(
            salePaymentRepository,
            mock(SalePaymentVoidRepository::class.java),
            saleDataFetcher,
            saleUpdater,
            salePaymentFetcher,
            salePaymentWriter(),
            mock(SalePaymentVoidHandlerForKafka::class.java),
            paymentMethodServiceNamingTheMethod(),
            mock(FiscalPeriodService::class.java)
        )

        salePaymentService.voidPayment(SalePaymentVoidCreateDto(payment.id!!, "entered twice"))

        verify(saleUpdater).updatePaymentStatus(saleId, PaymentStatus.UNPAID)
    }

    @Test
    fun `voidPayment takes the sale lock before checking whether the payment was already voided`() {
        val salePaymentRepository = mock(SalePaymentRepository::class.java)
        val salePaymentVoidRepository = mock(SalePaymentVoidRepository::class.java)
        val saleDataFetcher = mock(SaleDataFetcher::class.java)
        val payment = salePaymentEntity("100")
        `when`(salePaymentRepository.getReferenceById(payment.id!!)).thenReturn(payment)
        `when`(saleDataFetcher.lockAndGetSaleContext(saleId)).thenReturn(SaleContext(contactId, BigDecimal("100"), SaleStatus.CONFIRMED))
        `when`(salePaymentVoidRepository.existsBySalePaymentId(payment.id!!)).thenReturn(true)
        val salePaymentService = SalePaymentService(
            salePaymentRepository,
            salePaymentVoidRepository,
            saleDataFetcher,
            mock(SaleUpdater::class.java),
            salePaymentFetcher,
            salePaymentWriter(),
            mock(SalePaymentVoidHandlerForKafka::class.java),
            paymentMethodServiceNamingTheMethod(),
            mock(FiscalPeriodService::class.java)
        )

        assertThrows(RtsGenericException::class.java) {
            salePaymentService.voidPayment(SalePaymentVoidCreateDto(payment.id!!, "entered twice"))
        }

        val lockBeforeCheck = inOrder(saleDataFetcher, salePaymentVoidRepository)
        lockBeforeCheck.verify(saleDataFetcher).lockAndGetSaleContext(saleId)
        lockBeforeCheck.verify(salePaymentVoidRepository).existsBySalePaymentId(payment.id!!)
    }

    @Test
    fun `voidPayment keeps PARTIALLY_SETTLED when other active payments remain`() {
        val salePaymentRepository = mock(SalePaymentRepository::class.java)
        val saleDataFetcher = mock(SaleDataFetcher::class.java)
        val saleUpdater = mock(SaleUpdater::class.java)
        val payment = salePaymentEntity("40")
        `when`(salePaymentRepository.getReferenceById(payment.id!!)).thenReturn(payment)
        `when`(saleDataFetcher.lockAndGetSaleContext(saleId)).thenReturn(SaleContext(contactId, BigDecimal("100"), SaleStatus.CONFIRMED))
        `when`(salePaymentFetcher.calculatePaidAmount(saleId)).thenReturn(BigDecimal("100"))
        val salePaymentService = SalePaymentService(
            salePaymentRepository,
            mock(SalePaymentVoidRepository::class.java),
            saleDataFetcher,
            saleUpdater,
            salePaymentFetcher,
            salePaymentWriter(),
            mock(SalePaymentVoidHandlerForKafka::class.java),
            paymentMethodServiceNamingTheMethod(),
            mock(FiscalPeriodService::class.java)
        )

        salePaymentService.voidPayment(SalePaymentVoidCreateDto(payment.id!!, "wrong method"))

        verify(saleUpdater).updatePaymentStatus(saleId, PaymentStatus.PARTIALLY_SETTLED)
    }
}
