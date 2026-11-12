package me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance

import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactStatus
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactType
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactOpeningBalanceService
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactOpeningBalanceSetDto
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactSubledgerDirection
import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.organizations.business.lock.api.OrgEntityAdvisoryLock
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.ui_models.BalanceEffect
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

/** Revisions are append-only deltas; a wrong delta, a skipped lock or a missing guard corrupts the control accounts. */
class ContactOpeningBalanceServiceTest {

    private val latestRevisionsBySubledgerDirection = mutableMapOf<ContactSubledgerDirection, ContactOpeningBalanceEntity>()
    private val savedRevisions = mutableListOf<ContactOpeningBalanceEntity>()
    private val historyRows = mutableListOf<ContactOpeningBalanceEntity>()
    private val contactOpeningBalanceRepository = Proxy.newProxyInstance(
        ContactOpeningBalanceRepository::class.java.classLoader,
        arrayOf(ContactOpeningBalanceRepository::class.java)
    ) { _, method, arguments ->
        when (method.name) {
            "save" -> (arguments[0] as ContactOpeningBalanceEntity).also {
                it.referenceNumber = "COB000001"
                savedRevisions.add(it)
            }
            "findByContactReferenceNumberOrderByCreatedOnAsc" -> historyRows
            else -> throw UnsupportedOperationException(method.name)
        }
    } as ContactOpeningBalanceRepository

    private val contactService = mock(ContactService::class.java)
    private val ledgerPostingService = mock(LedgerPostingService::class.java)
    private val orgEntityAdvisoryLock = mock(OrgEntityAdvisoryLock::class.java)
    private val userQualifier = mock(UserQualifier::class.java)
    private var activeSettledAmount = BigDecimal.ZERO
    private val contactOpeningSettlementFinder = mock(ContactOpeningSettlementFinder::class.java) { invocation ->
        when (invocation.method.name) {
            "declaredAmount" -> latestRevisionsBySubledgerDirection[invocation.arguments[1]]?.amount ?: BigDecimal.ZERO
            "activeSettledAmount" -> activeSettledAmount
            else -> throw UnsupportedOperationException(invocation.method.name)
        }
    }
    private val contactOpeningBalanceService = ContactOpeningBalanceService(
        contactOpeningBalanceRepository,
        contactService,
        ledgerPostingService,
        contactOpeningSettlementFinder,
        orgEntityAdvisoryLock,
        mock(FiscalPeriodService::class.java),
        userQualifier
    )

    private val contactId = UUID.randomUUID()
    private val contactReferenceNumber = "CON000001"

    @BeforeEach
    fun setUp() {
        SessionContextProvider.setSession(
            SessionContext(organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"))
        )
        givenContact(ContactStatus.ACTIVE)
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `the first revision posts the whole amount as the delta`() {
        contactOpeningBalanceService.set(contactId, setDto(payable = "0", receivable = "100.00"))

        val saved = savedRevisions.single()
        assertEquals(ContactSubledgerDirection.RECEIVABLE, saved.subledgerDirection)
        assertEquals(listOf("100.00"), postedSignedDeltas())
        assertTrue(lockInvocations().single().arguments.toList() == listOf(LockNamespaces.CONTACT_OPENING_BALANCE, contactReferenceNumber))
    }

    @Test
    fun `payable and receivable are revised together under one lock`() {
        contactOpeningBalanceService.set(contactId, setDto(payable = "40.00", receivable = "100.00"))

        assertEquals(
            listOf(ContactSubledgerDirection.RECEIVABLE, ContactSubledgerDirection.PAYABLE),
            savedRevisions.map { it.subledgerDirection }
        )
        assertEquals(listOf("100.00", "40.00"), postedSignedDeltas())
        assertEquals(1, lockInvocations().size)
    }

    @Test
    fun `a side that is not provided keeps its latest revision`() {
        givenLatestRevision(revisionRow(ContactSubledgerDirection.PAYABLE, "100.00"))

        contactOpeningBalanceService.set(contactId, setDto(receivable = "20.00"))

        assertEquals(ContactSubledgerDirection.RECEIVABLE, savedRevisions.single().subledgerDirection)
        assertEquals(listOf("20.00"), postedSignedDeltas())
    }

    @Test
    fun `an explicitly empty side clears its latest revision to zero`() {
        givenLatestRevision(revisionRow(ContactSubledgerDirection.PAYABLE, "100.00"))

        contactOpeningBalanceService.set(contactId, ContactOpeningBalanceSetDto(payable = Optional.empty()))

        assertEquals("0.00", savedRevisions.single().amount.toPlainString())
        assertEquals(listOf("-100.00"), postedSignedDeltas())
    }

    @Test
    fun `a negative amount on either side rejects both`() {
        assertThrows(RtsGenericException::class.java) {
            contactOpeningBalanceService.set(contactId, setDto(payable = "-1.00", receivable = "100.00"))
        }
        assertTrue(savedRevisions.isEmpty())
    }

    @Test
    fun `a revision upward and downward posts the signed delta against the previous revision`() {
        givenLatestRevision(revisionRow(ContactSubledgerDirection.PAYABLE, "100.00"))
        contactOpeningBalanceService.set(contactId, setDto(payable = "130.00", receivable = "0"))
        givenLatestRevision(savedRevisions.last())
        contactOpeningBalanceService.set(contactId, setDto(payable = "90.00", receivable = "0"))

        assertEquals(2, savedRevisions.size)
        assertEquals(listOf("30.00", "-40.00"), postedSignedDeltas())
    }

    @Test
    fun `a zero delta records no revision and posts nothing`() {
        givenLatestRevision(revisionRow(ContactSubledgerDirection.RECEIVABLE, "100.00"))

        contactOpeningBalanceService.set(contactId, setDto(payable = "0", receivable = "100.00"))

        assertTrue(savedRevisions.isEmpty())
        assertTrue(postedSignedDeltas().isEmpty())
    }

    @Test
    fun `an amount beyond scale 2 is rejected before any revision is written`() {
        assertThrows(RtsGenericException::class.java) { setDto(payable = "10.005") }
        assertTrue(savedRevisions.isEmpty())
    }

    @Test
    fun `each subledger direction is revised against its own latest revision`() {
        givenLatestRevision(revisionRow(ContactSubledgerDirection.RECEIVABLE, "100.00"))

        contactOpeningBalanceService.set(contactId, setDto(payable = "40.00", receivable = "100.00"))

        assertEquals(ContactSubledgerDirection.PAYABLE, savedRevisions.single().subledgerDirection)
        assertEquals(listOf("40.00"), postedSignedDeltas())
    }

    @Test
    fun `a side cannot be lowered below the amount already settled against it, and neither side is written`() {
        givenLatestRevision(revisionRow(ContactSubledgerDirection.RECEIVABLE, "100.00"))
        activeSettledAmount = BigDecimal("60.00")

        assertThrows(RtsGenericException::class.java) {
            contactOpeningBalanceService.set(contactId, setDto(payable = "40.00", receivable = "50.00"))
        }
        assertTrue(savedRevisions.isEmpty())

        contactOpeningBalanceService.set(contactId, setDto(payable = "60.00", receivable = "60.00"))
        assertEquals(listOf("60.00"), savedRevisions.map { it.amount.toPlainString() }.take(1))
    }

    @Test
    fun `an inactive contact is rejected`() {
        givenContact(ContactStatus.INACTIVE)

        assertThrows(RtsGenericException::class.java) {
            contactOpeningBalanceService.set(contactId, setDto(payable = "0", receivable = "10.00"))
        }
        assertTrue(savedRevisions.isEmpty())
    }

    @Test
    fun `history lists each claimed amount with its direction, balance effect and author`() {
        val historyCreator = UUID.randomUUID()
        `when`(userQualifier.getUserFullName(historyCreator)).thenReturn("Ada Admin")
        historyRows += listOf(
            historyRow(ContactSubledgerDirection.RECEIVABLE, "100.0000", "COB000001", historyCreator),
            historyRow(ContactSubledgerDirection.PAYABLE, "40.0000", "COB000002", historyCreator),
            historyRow(ContactSubledgerDirection.PAYABLE, "0.0000", "COB000003", historyCreator)
        )

        val history = contactOpeningBalanceService.getHistory(contactReferenceNumber)

        assertEquals(listOf("COB000001", "COB000002", "COB000003"), history.map { it.referenceNumber })
        assertEquals(
            listOf(ContactSubledgerDirection.RECEIVABLE, ContactSubledgerDirection.PAYABLE, ContactSubledgerDirection.PAYABLE),
            history.map { it.subledgerDirection }
        )
        assertEquals(listOf("100.0000", "40.0000", "0.0000"), history.map { it.amount.toPlainString() })
        assertEquals(
            listOf(BalanceEffect.ADDS, BalanceEffect.SUBTRACTS, BalanceEffect.ZERO),
            history.map { it.balanceEffect }
        )
        assertEquals("Ada Admin", history.first().changedBy)
        assertEquals(contactReferenceNumber, history.first().contactReferenceNumber)
    }

    private fun historyRow(
        subledgerDirection: ContactSubledgerDirection,
        amount: String,
        referenceNumber: String,
        createdById: UUID
    ) = revisionRow(subledgerDirection, amount).also {
        it.referenceNumber = referenceNumber
        it.createdById = createdById
        it.createdOn = OffsetDateTime.now()
    }

    private fun givenContact(contactStatus: ContactStatus) {
        `when`(contactService.getContactById(contactId)).thenReturn(
            ContactDto(
                id = contactId,
                createdById = UUID.randomUUID(),
                createdOn = OffsetDateTime.now(),
                referenceNumber = contactReferenceNumber,
                contactTypes = setOf(ContactType.CUSTOMER),
                firstName = "Test",
                status = contactStatus
            )
        )
    }

    private fun givenLatestRevision(latestRevision: ContactOpeningBalanceEntity) {
        latestRevisionsBySubledgerDirection[latestRevision.subledgerDirection] = latestRevision
    }

    private fun revisionRow(subledgerDirection: ContactSubledgerDirection, amount: String) = ContactOpeningBalanceEntity(
        contactReferenceNumber = contactReferenceNumber,
        subledgerDirection = subledgerDirection,
        amount = BigDecimal(amount)
    )

    private fun setDto(payable: String? = null, receivable: String? = null) = ContactOpeningBalanceSetDto(
        payable = payable?.let { Optional.of(Money.of(BigDecimal(it))) },
        receivable = receivable?.let { Optional.of(Money.of(BigDecimal(it))) }
    )

    private fun postedSignedDeltas(): List<String> =
        mockingDetails(ledgerPostingService).invocations
            .filter { it.method.name == "post" }
            .map { signedDeltaOf(it.arguments[0] as LedgerPostingRequest) }

    private fun signedDeltaOf(ledgerPostingRequest: LedgerPostingRequest): String {
        val controlEntry = ledgerPostingRequest.entries.single {
            it.accountCode != SystemAccount.OPENING_BALANCE_EQUITY.code
        }
        val controlAccountIsReceivables = controlEntry.accountCode == SystemAccount.TRADE_RECEIVABLES.code
        val controlEntryIsDebit = controlEntry.entryType == EntryType.DEBIT
        val raisesControlBalance = controlAccountIsReceivables == controlEntryIsDebit
        val amount = controlEntry.amount.toBigDecimal().setScale(2)
        return (if (raisesControlBalance) amount else amount.negate()).toPlainString()
    }

    private fun lockInvocations() =
        mockingDetails(orgEntityAdvisoryLock).invocations.filter { it.method.name == "acquire" }
}
