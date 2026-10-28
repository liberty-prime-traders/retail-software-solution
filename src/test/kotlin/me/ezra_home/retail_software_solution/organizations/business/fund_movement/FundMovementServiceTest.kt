package me.ezra_home.retail_software_solution.organizations.business.fund_movement

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.api.FundMovementCapitalRequest
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.api.FundMovementDrawRequest
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.api.FundMovementResponse
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.api.FundMovementService
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.api.FundMovementTransferRequest
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.api.FundMovementType
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.api.FundMovementVoidRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.organizations.business.lock.api.OrgEntityAdvisoryLock
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.business.PositiveMoney
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
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
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class FundMovementServiceTest {

    private val fundMovementRepository = mock(FundMovementRepository::class.java)
    private val fundMovementVoidRepository = mock(FundMovementVoidRepository::class.java)
    private val fundMovementMapper = mock(FundMovementMapper::class.java)
    private val fundMovementAccountValidator = mock(FundMovementAccountValidator::class.java)
    private val fundMovementResponseBuilder = mock(FundMovementResponseBuilder::class.java)
    private val ledgerPostingService = mock(LedgerPostingService::class.java)
    private val orgEntityAdvisoryLock = mock(OrgEntityAdvisoryLock::class.java)

    private val fundMovementService = FundMovementService(
        fundMovementRepository, fundMovementVoidRepository, fundMovementMapper, fundMovementAccountValidator,
        fundMovementResponseBuilder, ledgerPostingService, orgEntityAdvisoryLock
    )

    private val postedRequests = mutableListOf<LedgerPostingRequest>()
    private val movementDate = LocalDate.of(2026, 3, 1)
    private val cashCode = SystemAccount.CASH.code
    private val digitalPaymentsCode = SystemAccount.DIGITAL_PAYMENTS.code

    @BeforeEach
    fun setUp() {
        SessionContextProvider.setSession(SessionContext(organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC")))
        doAnswer { invocation -> postedRequests.add(invocation.arguments[0] as LedgerPostingRequest); null }
            .`when`(ledgerPostingService).post(anyNonNull(LedgerPostingRequest::class.java))
        `when`(fundMovementRepository.save(any(FundMovementEntity::class.java))).thenAnswer { invocation ->
            (invocation.arguments[0] as FundMovementEntity).also { stamp(it) }
        }
        `when`(fundMovementVoidRepository.save(any(FundMovementVoidEntity::class.java))).thenAnswer { invocation ->
            (invocation.arguments[0] as FundMovementVoidEntity).also {
                it.id = UUID.randomUUID()
                it.referenceNumber = "FMVD000001"
                it.createdById = UUID.randomUUID()
                it.createdOn = OffsetDateTime.now()
            }
        }
        `when`(fundMovementMapper.toDto(anyNonNull(FundMovementEntity::class.java))).thenAnswer { toDto(it.arguments[0] as FundMovementEntity) }
        `when`(fundMovementMapper.toVoidDto(anyNonNull(FundMovementVoidEntity::class.java))).thenAnswer { invocation ->
            val entity = invocation.arguments[0] as FundMovementVoidEntity
            FundMovementVoidDto(entity.id!!, entity.requiredReference(), entity.fundMovementId, entity.voidDate, entity.reason, entity.requiredCreatedById(), entity.requiredCreatedOn())
        }
        `when`(fundMovementResponseBuilder.buildResponses(anyList())).thenReturn(listOf(mock(FundMovementResponse::class.java)))
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `a transfer to the same account is rejected before anything is saved or posted`() {
        assertThrows(RtsGenericException::class.java) {
            fundMovementService.createTransfer(FundMovementTransferRequest(cashCode, cashCode, PositiveMoney.of(BigDecimal("10")), movementDate))
        }

        verify(fundMovementRepository, never()).save(any(FundMovementEntity::class.java))
        assertTrue(postedRequests.isEmpty())
    }

    @Test
    fun `a transfer debits the destination and credits the source`() {
        fundMovementService.createTransfer(FundMovementTransferRequest(cashCode, digitalPaymentsCode, PositiveMoney.of(BigDecimal("10")), movementDate))

        val saved = savedEntity()
        assertEquals(FundMovementType.TRANSFER_BETWEEN_ACCOUNTS, saved.type)
        assertEquals(digitalPaymentsCode, saved.debitAccountCode)
        assertEquals(cashCode, saved.creditAccountCode)
        verify(fundMovementAccountValidator).requireSelectable(listOf(cashCode, digitalPaymentsCode))
    }

    @Test
    fun `capital debits the picked account and credits owners capital`() {
        fundMovementService.createCapital(FundMovementCapitalRequest(cashCode, PositiveMoney.of(BigDecimal("10")), movementDate))

        val saved = savedEntity()
        assertEquals(cashCode, saved.debitAccountCode)
        assertEquals(SystemAccount.OWNERS_CAPITAL.code, saved.creditAccountCode)
        verify(fundMovementAccountValidator).requireSelectable(listOf(cashCode))
    }

    @Test
    fun `a draw debits owners draws, credits the picked account and blanks the note`() {
        fundMovementService.createDraw(FundMovementDrawRequest(cashCode, PositiveMoney.of(BigDecimal("10.12")), movementDate, "   "))

        val saved = savedEntity()
        assertEquals(SystemAccount.OWNERS_DRAWS.code, saved.debitAccountCode)
        assertEquals(cashCode, saved.creditAccountCode)
        assertEquals(BigDecimal("10.12"), saved.amount)
        assertNull(saved.note)

        val posted = postedRequests.single()
        assertEquals("FMOV000001", posted.sourceReferenceNumber)
        assertEquals(movementDate, posted.postingDate)
    }

    @Test
    fun `zero, negative and amounts that round to zero are rejected`() {
        listOf("0", "-5", "0.00004").forEach { amount ->
            assertThrows(RtsGenericException::class.java) {
                fundMovementService.createCapital(FundMovementCapitalRequest(cashCode, PositiveMoney.of(BigDecimal(amount)), movementDate))
            }
        }
        verify(fundMovementRepository, never()).save(any(FundMovementEntity::class.java))
    }

    @Test
    fun `the transaction maximum itself is accepted`() {
        fundMovementService.createCapital(FundMovementCapitalRequest(cashCode, PositiveMoney.of(Decimals.MAXIMUM_TRANSACTION_AMOUNT), movementDate))

        assertEquals(Decimals.MAXIMUM_TRANSACTION_AMOUNT, savedEntity().amount)
    }

    @Test
    fun `a long note is kept in full`() {
        val longNote = "n".repeat(2000)

        fundMovementService.createCapital(FundMovementCapitalRequest(cashCode, PositiveMoney.of(BigDecimal("10")), movementDate, longNote))

        assertEquals(longNote, savedEntity().note)
    }

    @Test
    fun `a movement dated after today in the organization zone is rejected before anything is saved or posted`() {
        val tomorrow = DateTimes.Local.Now.organization().plusDays(1)

        assertThrows(RtsGenericException::class.java) {
            fundMovementService.createCapital(FundMovementCapitalRequest(cashCode, PositiveMoney.of(BigDecimal("10")), tomorrow))
        }

        verify(fundMovementRepository, never()).save(any(FundMovementEntity::class.java))
        assertTrue(postedRequests.isEmpty())
    }

    @Test
    fun `an account the selection rule rejects stops the movement before it is saved`() {
        doThrow(RtsGenericException("Fund movement account is inactive")).`when`(fundMovementAccountValidator).requireSelectable(listOf(cashCode))

        assertThrows(RtsGenericException::class.java) {
            fundMovementService.createCapital(FundMovementCapitalRequest(cashCode, PositiveMoney.of(BigDecimal("10")), movementDate))
        }

        verify(fundMovementRepository, never()).save(any(FundMovementEntity::class.java))
    }

    @Test
    fun `a ledger posting failure reaches the caller and the service rolls back on any exception`() {
        doThrow(RtsGenericException("No open fiscal period")).`when`(ledgerPostingService).post(anyNonNull(LedgerPostingRequest::class.java))

        assertThrows(RtsGenericException::class.java) {
            fundMovementService.createDraw(FundMovementDrawRequest(cashCode, PositiveMoney.of(BigDecimal("10")), movementDate))
        }

        val transactional = FundMovementService::class.java.getAnnotation(TransactionalOnOrganizationSchema::class.java)
        assertNotNull(transactional)
        assertEquals(listOf(Exception::class), transactional.rollbackFor.toList())
    }

    @Test
    fun `voiding locks the movement before checking for an existing void, then posts the reversal dated today`() {
        val entity = FundMovementEntity(FundMovementType.WITHDRAW_FOR_PERSONAL_USE, SystemAccount.OWNERS_DRAWS.code, cashCode, BigDecimal("10.0000"), movementDate, null).also { stamp(it) }
        `when`(fundMovementRepository.findById(entity.id!!)).thenReturn(Optional.of(entity))
        `when`(fundMovementVoidRepository.existsByFundMovementId(entity.id!!)).thenReturn(false)

        fundMovementService.voidFundMovement(FundMovementVoidRequest(entity.id!!, "  typo "))

        val lockThenCheck = inOrder(orgEntityAdvisoryLock, fundMovementVoidRepository)
        lockThenCheck.verify(orgEntityAdvisoryLock).acquire(LockNamespaces.FUND_MOVEMENT, entity.id.toString())
        lockThenCheck.verify(fundMovementVoidRepository).existsByFundMovementId(entity.id!!)

        val voidCaptor = ArgumentCaptor.forClass(FundMovementVoidEntity::class.java)
        verify(fundMovementVoidRepository).save(voidCaptor.capture())
        assertEquals("typo", voidCaptor.value.reason)

        val posted = postedRequests.single()
        assertEquals(voidCaptor.value.voidDate, posted.postingDate)
    }

    @Test
    fun `a movement that is already voided, or does not exist, cannot be voided`() {
        val entity = FundMovementEntity(FundMovementType.WITHDRAW_FOR_PERSONAL_USE, SystemAccount.OWNERS_DRAWS.code, cashCode, BigDecimal("10.0000"), movementDate, null).also { stamp(it) }
        `when`(fundMovementRepository.findById(entity.id!!)).thenReturn(Optional.of(entity))
        `when`(fundMovementVoidRepository.existsByFundMovementId(entity.id!!)).thenReturn(true)

        assertThrows(RtsGenericException::class.java) { fundMovementService.voidFundMovement(FundMovementVoidRequest(entity.id!!)) }
        val unknownId = UUID.randomUUID()
        `when`(fundMovementRepository.findById(unknownId)).thenReturn(Optional.empty())
        assertThrows(RtsGenericException::class.java) { fundMovementService.voidFundMovement(FundMovementVoidRequest(unknownId)) }

        verify(fundMovementVoidRepository, never()).save(any(FundMovementVoidEntity::class.java))
        assertTrue(postedRequests.isEmpty())
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNonNull(type: Class<T>): T {
        any(type)
        return null as T
    }

    private fun savedEntity(): FundMovementEntity {
        val captor = ArgumentCaptor.forClass(FundMovementEntity::class.java)
        verify(fundMovementRepository).save(captor.capture())
        return captor.value
    }

    private fun stamp(entity: FundMovementEntity) {
        entity.id = UUID.randomUUID()
        entity.referenceNumber = "FMOV000001"
        entity.createdById = UUID.randomUUID()
        entity.createdOn = OffsetDateTime.now()
    }

    private fun toDto(entity: FundMovementEntity) = FundMovementDto(
        entity.id!!, entity.requiredReference(), entity.type, entity.debitAccountCode, entity.creditAccountCode,
        entity.amount, entity.movementDate, entity.note, entity.requiredCreatedById(), entity.requiredCreatedOn()
    )
}
