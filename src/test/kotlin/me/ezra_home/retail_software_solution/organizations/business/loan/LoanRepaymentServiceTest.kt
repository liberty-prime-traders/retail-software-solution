package me.ezra_home.retail_software_solution.organizations.business.loan

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.organizations.business.loan.LoanTestSupport.anyNonNull
import me.ezra_home.retail_software_solution.organizations.business.loan.LoanTestSupport.stamp
import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanRepaymentCreateRequest
import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanRepaymentService
import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanRepaymentVoidRequest
import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanResponse
import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanStatus
import me.ezra_home.retail_software_solution.organizations.business.lock.api.OrgEntityAdvisoryLock
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.PositiveMoney
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.business.mappers.StringQualifier
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class LoanRepaymentServiceTest {

    private val loanRepository = mock(LoanRepository::class.java)
    private val loanRepaymentRepository = mock(LoanRepaymentRepository::class.java)
    private val loanStatusRepository = mock(LoanStatusRepository::class.java)
    private val loanVoidRepository = mock(LoanVoidRepository::class.java)
    private val loanRepaymentVoidRepository = mock(LoanRepaymentVoidRepository::class.java)
    private val loanAccountValidator = mock(LoanAccountValidator::class.java)
    private val loanResponseBuilder = mock(LoanResponseBuilder::class.java)
    private val contactService = mock(ContactService::class.java)
    private val ledgerPostingService = mock(LedgerPostingService::class.java)
    private val orgEntityAdvisoryLock = mock(OrgEntityAdvisoryLock::class.java)

    private val loanMapper = LoanMapperImpl(StringQualifier)

    private val loanRepaymentService = LoanRepaymentService(
        loanRepaymentRepository, loanRepaymentVoidRepository, loanMapper,
        LoanFinder(loanRepository, loanStatusRepository, loanMapper, contactService, orgEntityAdvisoryLock),
        loanAccountValidator,
        LoanRepaymentValidator(loanVoidRepository, loanRepaymentVoidRepository),
        loanResponseBuilder, ledgerPostingService
    )

    private val postedRequests = mutableListOf<LedgerPostingRequest>()
    private val loanStatusFiguresPassedToBuilder = mutableListOf<LoanStatusFigures>()
    private val loanDate = LocalDate.of(2026, 3, 1)
    private val cashCode = SystemAccount.CASH.code
    private val lenderContactDto = LoanTestSupport.lenderContactDto()
    private val loanEntity = LoanEntity(lenderContactDto.id, cashCode, BigDecimal("100.0000"), loanDate, null).also { stamp(it, "LOAN000001") }
    private val loanId = loanEntity.id!!

    @BeforeEach
    fun setUp() {
        SessionContextProvider.setSession(SessionContext(organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC")))
        doAnswer { invocation -> postedRequests.add(invocation.arguments[0] as LedgerPostingRequest); null }
            .`when`(ledgerPostingService).post(anyNonNull(LedgerPostingRequest::class.java))
        `when`(contactService.getContactById(lenderContactDto.id)).thenReturn(lenderContactDto)
        `when`(loanRepaymentRepository.save(any(LoanRepaymentEntity::class.java))).thenAnswer { invocation ->
            (invocation.arguments[0] as LoanRepaymentEntity).also { stamp(it, "LNRP000002") }
        }
        `when`(loanRepaymentVoidRepository.save(any(LoanRepaymentVoidEntity::class.java))).thenAnswer { invocation ->
            (invocation.arguments[0] as LoanRepaymentVoidEntity).also { stamp(it, "LRPV000001") }
        }
        `when`(loanRepository.findById(loanId)).thenReturn(Optional.of(loanEntity))
        outstandingIs("100")
        `when`(
            loanResponseBuilder.buildResponse(
                anyNonNull(LoanDto::class.java), anyNonNull(ContactDto::class.java), anyNonNull(LoanStatusFigures::class.java)
            )
        ).thenAnswer { invocation ->
            loanStatusFiguresPassedToBuilder.add(invocation.arguments[2] as LoanStatusFigures)
            mock(LoanResponse::class.java)
        }
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `zero, negative and amounts that round to zero are rejected`() {
        listOf("0", "-5", "0.00004").forEach { amount ->
            assertThrows(RtsGenericException::class.java) {
                loanRepaymentService.recordRepayment(LoanRepaymentCreateRequest(loanId, cashCode, PositiveMoney.of(BigDecimal(amount)), loanDate))
            }
        }

        verify(loanRepaymentRepository, never()).save(any(LoanRepaymentEntity::class.java))
        assertTrue(postedRequests.isEmpty())
    }

    @Test
    fun `an asset account the selection rule rejects stops a repayment before it is saved`() {
        doThrow(RtsGenericException("Loan account is inactive")).`when`(loanAccountValidator).requireSelectable(cashCode)

        assertThrows(RtsGenericException::class.java) {
            loanRepaymentService.recordRepayment(LoanRepaymentCreateRequest(loanId, cashCode, PositiveMoney.of(BigDecimal("10")), loanDate))
        }

        verify(loanRepaymentRepository, never()).save(any(LoanRepaymentEntity::class.java))
    }

    @Test
    fun `a repayment locks the loan before reading the outstanding balance, and posts after saving`() {
        loanRepaymentService.recordRepayment(LoanRepaymentCreateRequest(loanId, cashCode, PositiveMoney.of(BigDecimal("40")), loanDate, " paid "))

        val order = inOrder(orgEntityAdvisoryLock, loanStatusRepository, loanRepaymentRepository, ledgerPostingService)
        order.verify(orgEntityAdvisoryLock).acquire(LockNamespaces.LOAN, loanId.toString())
        order.verify(loanStatusRepository, times(1)).findById(loanId)
        order.verify(loanRepaymentRepository).save(any(LoanRepaymentEntity::class.java))
        order.verify(ledgerPostingService).post(anyNonNull(LedgerPostingRequest::class.java))

        val savedRepayment = savedRepayment()
        assertEquals(loanId, savedRepayment.loanId)
        assertEquals(BigDecimal("40.00"), savedRepayment.amount)
        assertEquals("paid", savedRepayment.note)

        val posted = postedRequests.single()
        assertEquals(listOf(SystemAccount.LOANS_PAYABLE.code, cashCode), posted.entries.map { it.accountCode })
        assertEquals(0, BigDecimal("40").compareTo(posted.subledgerEntries.single().receivableAmount.toBigDecimal()))
    }

    @Test
    fun `a repayment above the outstanding balance names the amount requested and the outstanding amount`() {
        outstandingIs("30.00")

        val exception = assertThrows(RtsGenericException::class.java) {
            loanRepaymentService.recordRepayment(LoanRepaymentCreateRequest(loanId, cashCode, PositiveMoney.of(BigDecimal("30.01")), loanDate))
        }

        assertTrue(exception.message.contains("30.01"), exception.message)
        assertTrue(exception.message.contains("30.00"), exception.message)
        verify(loanRepaymentRepository, never()).save(any(LoanRepaymentEntity::class.java))
    }

    @Test
    fun `a repayment of exactly the outstanding balance is accepted`() {
        outstandingIs("30.00")

        loanRepaymentService.recordRepayment(LoanRepaymentCreateRequest(loanId, cashCode, PositiveMoney.of(BigDecimal("30")), loanDate))

        assertEquals(BigDecimal("30.00"), savedRepayment().amount)
    }

    @Test
    fun `a repayment against a voided loan, or an unknown one, is rejected`() {
        `when`(loanVoidRepository.existsByLoanId(loanId)).thenReturn(true)
        assertThrows(RtsGenericException::class.java) {
            loanRepaymentService.recordRepayment(LoanRepaymentCreateRequest(loanId, cashCode, PositiveMoney.of(BigDecimal("10")), loanDate))
        }
        val unknownLoanId = UUID.randomUUID()
        `when`(loanRepository.findById(unknownLoanId)).thenReturn(Optional.empty())
        assertThrows(RtsGenericException::class.java) {
            loanRepaymentService.recordRepayment(LoanRepaymentCreateRequest(unknownLoanId, cashCode, PositiveMoney.of(BigDecimal("10")), loanDate))
        }

        verify(loanRepaymentRepository, never()).save(any(LoanRepaymentEntity::class.java))
        assertTrue(postedRequests.isEmpty())
    }

    @Test
    fun `a repayment dated in the future is rejected, and today is accepted`() {
        val tomorrow = DateTimes.Local.Now.organization().plusDays(1)

        assertThrows(RtsGenericException::class.java) {
            loanRepaymentService.recordRepayment(LoanRepaymentCreateRequest(loanId, cashCode, PositiveMoney.of(BigDecimal("10")), tomorrow))
        }
        verify(loanRepaymentRepository, never()).save(any(LoanRepaymentEntity::class.java))
        assertTrue(postedRequests.isEmpty())

        loanRepaymentService.recordRepayment(LoanRepaymentCreateRequest(loanId, cashCode, PositiveMoney.of(BigDecimal("10")), DateTimes.Local.Now.organization()))
        assertEquals(1, postedRequests.size)
    }

    @Test
    fun `a repayment dated before its loan is rejected, and the loan date itself is accepted`() {
        assertThrows(RtsGenericException::class.java) {
            loanRepaymentService.recordRepayment(LoanRepaymentCreateRequest(loanId, cashCode, PositiveMoney.of(BigDecimal("10")), loanDate.minusDays(1)))
        }
        verify(loanRepaymentRepository, never()).save(any(LoanRepaymentEntity::class.java))
        assertTrue(postedRequests.isEmpty())

        loanRepaymentService.recordRepayment(LoanRepaymentCreateRequest(loanId, cashCode, PositiveMoney.of(BigDecimal("10")), loanDate))
        assertEquals(loanDate, savedRepayment().repaymentDate)
    }

    @Test
    fun `voiding a repayment locks its loan, then posts the original account and amount back`() {
        val repayment = LoanRepaymentEntity(loanId, SystemAccount.DIGITAL_PAYMENTS.code, BigDecimal("30.0000"), loanDate, null)
            .also { stamp(it, "LNRP000001") }
        `when`(loanRepaymentRepository.findById(repayment.id!!)).thenReturn(Optional.of(repayment))

        loanRepaymentService.voidRepayment(LoanRepaymentVoidRequest(repayment.id!!, "wrong account"))

        val order = inOrder(orgEntityAdvisoryLock, loanRepaymentVoidRepository, ledgerPostingService)
        order.verify(orgEntityAdvisoryLock).acquire(LockNamespaces.LOAN, loanId.toString())
        order.verify(loanRepaymentVoidRepository).existsByLoanRepaymentId(repayment.id!!)
        order.verify(ledgerPostingService).post(anyNonNull(LedgerPostingRequest::class.java))

        val posted = postedRequests.single()
        assertEquals("LNRP000001", posted.sourceReferenceNumber)
        assertEquals(listOf(SystemAccount.DIGITAL_PAYMENTS.code, SystemAccount.LOANS_PAYABLE.code), posted.entries.map { it.accountCode })
        assertEquals(0, BigDecimal("30").compareTo(posted.subledgerEntries.single().payableAmount.toBigDecimal()))
    }

    @Test
    fun `a repayment cannot be voided twice, and an unknown repayment cannot be voided`() {
        val repayment = LoanRepaymentEntity(loanId, cashCode, BigDecimal("30.0000"), loanDate, null).also { stamp(it, "LNRP000001") }
        `when`(loanRepaymentRepository.findById(repayment.id!!)).thenReturn(Optional.of(repayment))
        `when`(loanRepaymentVoidRepository.existsByLoanRepaymentId(repayment.id!!)).thenReturn(true)
        val unknownRepaymentId = UUID.randomUUID()
        `when`(loanRepaymentRepository.findById(unknownRepaymentId)).thenReturn(Optional.empty())

        assertThrows(RtsGenericException::class.java) { loanRepaymentService.voidRepayment(LoanRepaymentVoidRequest(repayment.id!!)) }
        assertThrows(RtsGenericException::class.java) { loanRepaymentService.voidRepayment(LoanRepaymentVoidRequest(unknownRepaymentId)) }

        verify(loanRepaymentVoidRepository, never()).save(any(LoanRepaymentVoidEntity::class.java))
        assertTrue(postedRequests.isEmpty())
    }

    @Test
    fun `a ledger posting failure reaches the caller and the service rolls back on any exception`() {
        doThrow(RtsGenericException("No open fiscal period")).`when`(ledgerPostingService).post(anyNonNull(LedgerPostingRequest::class.java))

        assertThrows(RtsGenericException::class.java) {
            loanRepaymentService.recordRepayment(LoanRepaymentCreateRequest(loanId, cashCode, PositiveMoney.of(BigDecimal("10")), loanDate))
        }

        val transactional = LoanRepaymentService::class.java.getAnnotation(TransactionalOnOrganizationSchema::class.java)
        assertNotNull(transactional)
        assertEquals(listOf(Exception::class), transactional.rollbackFor.toList())
    }

    @Test
    fun `a repayment returns the status moved by the amount, not a re-read of the view`() {
        loanRepaymentService.recordRepayment(LoanRepaymentCreateRequest(loanId, cashCode, PositiveMoney.of(BigDecimal("40")), loanDate))

        val loanStatusFigures = loanStatusFiguresPassedToBuilder.single()
        assertEquals(0, BigDecimal("40").compareTo(loanStatusFigures.amountRepaid))
        assertEquals(0, BigDecimal("60").compareTo(loanStatusFigures.outstanding))
        assertEquals(LoanStatus.OPEN, loanStatusFigures.status)
    }

    @Test
    fun `a repayment of the whole outstanding balance returns a settled loan`() {
        loanRepaymentService.recordRepayment(LoanRepaymentCreateRequest(loanId, cashCode, PositiveMoney.of(BigDecimal("100")), loanDate))

        val loanStatusFigures = loanStatusFiguresPassedToBuilder.single()
        assertEquals(0, BigDecimal("100").compareTo(loanStatusFigures.amountRepaid))
        assertEquals(0, loanStatusFigures.outstanding.signum())
        assertEquals(LoanStatus.SETTLED, loanStatusFigures.status)
    }

    @Test
    fun `voiding a repayment returns the status with the amount moved back to outstanding`() {
        val repayment = LoanRepaymentEntity(loanId, cashCode, BigDecimal("30.0000"), loanDate, null).also { stamp(it, "LNRP000001") }
        `when`(loanRepaymentRepository.findById(repayment.id!!)).thenReturn(Optional.of(repayment))
        `when`(loanStatusRepository.findById(loanId)).thenReturn(
            Optional.of(LoanStatusEntity(loanId, BigDecimal("100"), BigDecimal.ZERO, LoanStatus.SETTLED))
        )

        loanRepaymentService.voidRepayment(LoanRepaymentVoidRequest(repayment.id!!))

        val loanStatusFigures = loanStatusFiguresPassedToBuilder.single()
        assertEquals(0, BigDecimal("70").compareTo(loanStatusFigures.amountRepaid))
        assertEquals(0, BigDecimal("30").compareTo(loanStatusFigures.outstanding))
        assertEquals(LoanStatus.OPEN, loanStatusFigures.status)
    }

    private fun savedRepayment(): LoanRepaymentEntity {
        val captor = ArgumentCaptor.forClass(LoanRepaymentEntity::class.java)
        verify(loanRepaymentRepository).save(captor.capture())
        return captor.value
    }

    private fun outstandingIs(outstanding: String) {
        `when`(loanStatusRepository.findById(loanId)).thenReturn(
            Optional.of(LoanStatusEntity(loanId, BigDecimal.ZERO, BigDecimal(outstanding), LoanStatus.OPEN))
        )
    }
}
