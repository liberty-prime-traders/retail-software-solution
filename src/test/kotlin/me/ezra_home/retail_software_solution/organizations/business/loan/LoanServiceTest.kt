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
import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanCreateRequest
import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanResponse
import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanService
import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanVoidRequest
import me.ezra_home.retail_software_solution.organizations.business.lock.api.OrgEntityAdvisoryLock
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.business.mappers.StringQualifier
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class LoanServiceTest {

    private val loanRepository = mock(LoanRepository::class.java)
    private val activeLoanRepaymentRepository = mock(ActiveLoanRepaymentRepository::class.java)
    private val loanVoidRepository = mock(LoanVoidRepository::class.java)
    private val loanLenderValidator = mock(LoanLenderValidator::class.java)
    private val loanAccountValidator = mock(LoanAccountValidator::class.java)
    private val loanResponseBuilder = mock(LoanResponseBuilder::class.java)
    private val contactService = mock(ContactService::class.java)
    private val ledgerPostingService = mock(LedgerPostingService::class.java)
    private val orgEntityAdvisoryLock = mock(OrgEntityAdvisoryLock::class.java)

    private val loanMapper = LoanMapperImpl(StringQualifier)

    private val loanService = LoanService(
        loanRepository, loanVoidRepository, loanMapper,
        LoanFinder(loanRepository, loanMapper, contactService, orgEntityAdvisoryLock),
        loanLenderValidator, loanAccountValidator,
        LoanVoidValidator(loanVoidRepository, activeLoanRepaymentRepository),
        loanResponseBuilder, ledgerPostingService
    )

    private val postedRequests = mutableListOf<LedgerPostingRequest>()
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
        `when`(loanLenderValidator.requireActiveLender(lenderContactDto.referenceNumber)).thenReturn(lenderContactDto)
        `when`(contactService.getContactById(lenderContactDto.id)).thenReturn(lenderContactDto)
        `when`(loanRepository.save(any(LoanEntity::class.java))).thenAnswer { invocation ->
            (invocation.arguments[0] as LoanEntity).also { stamp(it, "LOAN000002") }
        }
        `when`(loanVoidRepository.save(any(LoanVoidEntity::class.java))).thenAnswer { invocation ->
            (invocation.arguments[0] as LoanVoidEntity).also { stamp(it, "LNVD000001") }
        }
        `when`(loanRepository.findById(loanId)).thenReturn(Optional.of(loanEntity))
        `when`(activeLoanRepaymentRepository.findByLoanIdOrderByCreatedOnAscIdAsc(loanId)).thenReturn(emptyList())
        `when`(loanResponseBuilder.buildResponse(anyNonNull(LoanDto::class.java), anyNonNull(ContactDto::class.java))).thenReturn(mock(LoanResponse::class.java))
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `a loan is saved rounded with a blank note dropped, then posted from the lender's side`() {
        loanService.createLoan(LoanCreateRequest(lenderContactDto.referenceNumber, cashCode, BigDecimal("50.12345"), loanDate, "   "))

        val savedLoan = savedLoan()
        assertEquals(lenderContactDto.id, savedLoan.lenderContactId)
        assertEquals(cashCode, savedLoan.assetAccountCode)
        assertEquals(BigDecimal("50.12"), savedLoan.amount)
        assertNull(savedLoan.note)
        verify(loanAccountValidator).requireSelectable(cashCode)

        val posted = postedRequests.single()
        assertEquals("LOAN000002", posted.sourceReferenceNumber)
        assertEquals(loanDate, posted.postingDate)
        assertEquals(listOf(cashCode, SystemAccount.LOANS_PAYABLE.code), posted.entries.map { it.accountCode })
        val subledgerEntry = posted.subledgerEntries.single()
        assertEquals(lenderContactDto.referenceNumber, subledgerEntry.contactReferenceNumber)
        assertEquals(BigDecimal("50.12"), subledgerEntry.payableAmount)
        assertEquals(0, BigDecimal.ZERO.compareTo(subledgerEntry.receivableAmount))
    }

    @Test
    fun `zero, negative and amounts that round to zero are rejected`() {
        listOf("0", "-5", "0.00004").forEach { amount ->
            assertThrows(RtsGenericException::class.java) {
                loanService.createLoan(LoanCreateRequest(lenderContactDto.referenceNumber, cashCode, BigDecimal(amount), loanDate))
            }
        }

        verify(loanRepository, never()).save(any(LoanEntity::class.java))
        assertTrue(postedRequests.isEmpty())
    }

    @Test
    fun `an amount above the transaction maximum is rejected, and the maximum itself is accepted`() {
        val tooLarge = Decimals.MAXIMUM_TRANSACTION_AMOUNT.add(BigDecimal("0.01"))

        assertThrows(RtsGenericException::class.java) {
            loanService.createLoan(LoanCreateRequest(lenderContactDto.referenceNumber, cashCode, tooLarge, loanDate))
        }
        verify(loanRepository, never()).save(any(LoanEntity::class.java))
        assertTrue(postedRequests.isEmpty())

        loanService.createLoan(LoanCreateRequest(lenderContactDto.referenceNumber, cashCode, Decimals.MAXIMUM_TRANSACTION_AMOUNT, loanDate))
        assertEquals(Decimals.MAXIMUM_TRANSACTION_AMOUNT, savedLoan().amount)
    }

    @Test
    fun `a long note and a long void reason are kept in full`() {
        val longText = "n".repeat(2000)
        loanService.createLoan(LoanCreateRequest(lenderContactDto.referenceNumber, cashCode, BigDecimal("10"), loanDate, longText))
        assertEquals(longText, savedLoan().note)

        loanService.voidLoan(LoanVoidRequest(loanId, longText))

        val voidCaptor = ArgumentCaptor.forClass(LoanVoidEntity::class.java)
        verify(loanVoidRepository).save(voidCaptor.capture())
        assertEquals(longText, voidCaptor.value.reason)
    }

    @Test
    fun `a lender the validator rejects stops the loan before it is saved`() {
        doThrow(RtsGenericException("Contact Acme is not a lender")).`when`(loanLenderValidator).requireActiveLender("CT000009")

        assertThrows(RtsGenericException::class.java) {
            loanService.createLoan(LoanCreateRequest("CT000009", cashCode, BigDecimal("10"), loanDate))
        }

        verify(loanRepository, never()).save(any(LoanEntity::class.java))
        assertTrue(postedRequests.isEmpty())
    }

    @Test
    fun `an asset account the selection rule rejects stops a loan before it is saved`() {
        doThrow(RtsGenericException("Loan account is inactive")).`when`(loanAccountValidator).requireSelectable(cashCode)

        assertThrows(RtsGenericException::class.java) {
            loanService.createLoan(LoanCreateRequest(lenderContactDto.referenceNumber, cashCode, BigDecimal("10"), loanDate))
        }

        verify(loanRepository, never()).save(any(LoanEntity::class.java))
    }

    @Test
    fun `a loan dated in the future is rejected, and today is accepted`() {
        val tomorrow = DateTimes.Local.Now.organization().plusDays(1)

        assertThrows(RtsGenericException::class.java) {
            loanService.createLoan(LoanCreateRequest(lenderContactDto.referenceNumber, cashCode, BigDecimal("10"), tomorrow))
        }
        verify(loanRepository, never()).save(any(LoanEntity::class.java))
        assertTrue(postedRequests.isEmpty())

        loanService.createLoan(LoanCreateRequest(lenderContactDto.referenceNumber, cashCode, BigDecimal("10"), DateTimes.Local.Now.organization()))
        assertEquals(1, postedRequests.size)
    }

    @Test
    fun `voiding a loan locks it, reads the original row and posts the reversal dated today`() {
        loanService.voidLoan(LoanVoidRequest(loanId, "  entered twice "))

        val order = inOrder(orgEntityAdvisoryLock, loanVoidRepository, ledgerPostingService)
        order.verify(orgEntityAdvisoryLock).acquire(LockNamespaces.LOAN, loanId.toString())
        order.verify(loanVoidRepository).existsByLoanId(loanId)
        order.verify(loanVoidRepository).save(any(LoanVoidEntity::class.java))
        order.verify(ledgerPostingService).post(anyNonNull(LedgerPostingRequest::class.java))

        val voidCaptor = ArgumentCaptor.forClass(LoanVoidEntity::class.java)
        verify(loanVoidRepository).save(voidCaptor.capture())
        assertEquals("entered twice", voidCaptor.value.reason)
        assertEquals(DateTimes.Local.Now.organization(), voidCaptor.value.voidDate)

        val posted = postedRequests.single()
        assertEquals("LOAN000001", posted.sourceReferenceNumber)
        assertEquals(voidCaptor.value.voidDate, posted.postingDate)
        assertEquals(listOf(SystemAccount.LOANS_PAYABLE.code, cashCode), posted.entries.map { it.accountCode })
        assertEquals(0, BigDecimal("100").compareTo(posted.subledgerEntries.single().receivableAmount))
    }

    @Test
    fun `a loan with a non-voided repayment cannot be voided and the error names the repayments`() {
        val activeRepayments = listOf("LNRP000001", "LNRP000002").map { reference -> activeRepayment("10.0000", reference) }
        `when`(activeLoanRepaymentRepository.findByLoanIdOrderByCreatedOnAscIdAsc(loanId)).thenReturn(activeRepayments)

        val exception = assertThrows(RtsGenericException::class.java) { loanService.voidLoan(LoanVoidRequest(loanId)) }

        assertTrue(exception.message.contains("LNRP000001") && exception.message.contains("LNRP000002"), exception.message)
        verify(loanVoidRepository, never()).save(any(LoanVoidEntity::class.java))
        assertTrue(postedRequests.isEmpty())
    }

    @Test
    fun `a loan cannot be voided twice`() {
        `when`(loanVoidRepository.existsByLoanId(loanId)).thenReturn(true)

        assertThrows(RtsGenericException::class.java) { loanService.voidLoan(LoanVoidRequest(loanId)) }

        verify(loanVoidRepository, never()).save(any(LoanVoidEntity::class.java))
        assertTrue(postedRequests.isEmpty())
    }

    // Rolling the rows back relies on the exception leaving the transactional service; the rollback itself needs a real database.
    @Test
    fun `a ledger posting failure reaches the caller and the service rolls back on any exception`() {
        doThrow(RtsGenericException("No open fiscal period")).`when`(ledgerPostingService).post(anyNonNull(LedgerPostingRequest::class.java))

        assertThrows(RtsGenericException::class.java) {
            loanService.createLoan(LoanCreateRequest(lenderContactDto.referenceNumber, cashCode, BigDecimal("10"), loanDate))
        }

        val transactional = LoanService::class.java.getAnnotation(TransactionalOnOrganizationSchema::class.java)
        assertNotNull(transactional)
        assertEquals(listOf(Exception::class), transactional.rollbackFor.toList())
    }

    private fun savedLoan(): LoanEntity {
        val captor = ArgumentCaptor.forClass(LoanEntity::class.java)
        verify(loanRepository).save(captor.capture())
        return captor.value
    }

    private fun activeRepayment(amount: String, reference: String) =
        ActiveLoanRepaymentEntity(UUID.randomUUID(), reference, loanId, BigDecimal(amount), OffsetDateTime.now())
}
