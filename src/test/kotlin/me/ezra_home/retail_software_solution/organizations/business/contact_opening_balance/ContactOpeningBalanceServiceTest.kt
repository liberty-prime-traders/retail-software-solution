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
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.organizations.business.lock.api.OrgEntityAdvisoryLock
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.ui_models.BalanceSignal
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.`when`
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
            "save" -> (arguments[0] as ContactOpeningBalanceEntity).also { savedRevisions.add(it) }
            "findByContactReferenceNumberOrderByCreatedOnAsc" -> historyRows
            "findFirstByContactReferenceNumberAndSubledgerDirectionOrderByCreatedOnDesc" -> latestRevisionsBySubledgerDirection[arguments[1]]
            else -> throw UnsupportedOperationException(method.name)
        }
    } as ContactOpeningBalanceRepository

    private val contactService = mock(ContactService::class.java)
    private val contactOpeningBalanceHandlerForKafka = mock(ContactOpeningBalanceHandlerForKafka::class.java)
    private val orgEntityAdvisoryLock = mock(OrgEntityAdvisoryLock::class.java)
    private val userQualifier = mock(UserQualifier::class.java)
    private val contactOpeningBalanceService = ContactOpeningBalanceService(
        contactOpeningBalanceRepository,
        contactService,
        contactOpeningBalanceHandlerForKafka,
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
    fun `the first revision publishes the whole amount as the delta`() {
        contactOpeningBalanceService.set(contactId, setDto(payable = "0", receivable = "100.00"))

        val saved = savedRevisions.single()
        assertEquals(ContactSubledgerDirection.RECEIVABLE, saved.subledgerDirection)
        assertEquals(listOf("100.0000"), publishedSignedDeltas())
        assertTrue(lockInvocations().single().arguments.toList() == listOf(LockNamespaces.CONTACT_OPENING_BALANCE, contactReferenceNumber))
    }

    @Test
    fun `payable and receivable are revised together under one lock`() {
        contactOpeningBalanceService.set(contactId, setDto(payable = "40.00", receivable = "100.00"))

        assertEquals(
            listOf(ContactSubledgerDirection.RECEIVABLE, ContactSubledgerDirection.PAYABLE),
            savedRevisions.map { it.subledgerDirection }
        )
        assertEquals(listOf("100.0000", "40.0000"), publishedSignedDeltas())
        assertEquals(1, lockInvocations().size)
    }

    @Test
    fun `a side that is not provided keeps its latest revision`() {
        givenLatestRevision(revisionRow(ContactSubledgerDirection.PAYABLE, "100.00"))

        contactOpeningBalanceService.set(contactId, setDto(receivable = "20.00"))

        assertEquals(ContactSubledgerDirection.RECEIVABLE, savedRevisions.single().subledgerDirection)
        assertEquals(listOf("20.0000"), publishedSignedDeltas())
    }

    @Test
    fun `an explicitly empty side clears its latest revision to zero`() {
        givenLatestRevision(revisionRow(ContactSubledgerDirection.PAYABLE, "100.00"))

        contactOpeningBalanceService.set(contactId, ContactOpeningBalanceSetDto(payable = Optional.empty()))

        assertEquals("0.0000", savedRevisions.single().amount.toPlainString())
        assertEquals(listOf("-100.0000"), publishedSignedDeltas())
    }

    @Test
    fun `a negative amount on either side rejects both`() {
        assertThrows(RtsGenericException::class.java) {
            contactOpeningBalanceService.set(contactId, setDto(payable = "-1.00", receivable = "100.00"))
        }
        assertTrue(savedRevisions.isEmpty())
    }

    @Test
    fun `a revision upward and downward publishes the signed delta against the previous revision`() {
        givenLatestRevision(revisionRow(ContactSubledgerDirection.PAYABLE, "100.00"))
        contactOpeningBalanceService.set(contactId, setDto(payable = "130.00", receivable = "0"))
        givenLatestRevision(savedRevisions.last())
        contactOpeningBalanceService.set(contactId, setDto(payable = "90.00", receivable = "0"))

        assertEquals(2, savedRevisions.size)
        assertEquals(listOf("30.0000", "-40.0000"), publishedSignedDeltas())
    }

    @Test
    fun `a zero delta records no revision and publishes nothing`() {
        givenLatestRevision(revisionRow(ContactSubledgerDirection.RECEIVABLE, "100.00"))

        contactOpeningBalanceService.set(contactId, setDto(payable = "0", receivable = "100.00"))

        assertTrue(savedRevisions.isEmpty())
        assertTrue(publishedSignedDeltas().isEmpty())
    }

    @Test
    fun `an amount beyond scale 4 is compared and stored at scale 4`() {
        givenLatestRevision(revisionRow(ContactSubledgerDirection.RECEIVABLE, "100.0001"))

        contactOpeningBalanceService.set(contactId, setDto(payable = "0", receivable = "100.00005"))

        assertTrue(savedRevisions.isEmpty())
        assertTrue(publishedSignedDeltas().isEmpty())

        contactOpeningBalanceService.set(contactId, setDto(payable = "10.00005", receivable = "100.00005"))

        assertEquals("10.0001", savedRevisions.single().amount.toPlainString())
        assertEquals(listOf("10.0001"), publishedSignedDeltas())
    }

    @Test
    fun `each subledger direction is revised against its own latest revision`() {
        givenLatestRevision(revisionRow(ContactSubledgerDirection.RECEIVABLE, "100.00"))

        contactOpeningBalanceService.set(contactId, setDto(payable = "40.00", receivable = "100.00"))

        assertEquals(ContactSubledgerDirection.PAYABLE, savedRevisions.single().subledgerDirection)
        assertEquals(listOf("40.0000"), publishedSignedDeltas())
    }

    @Test
    fun `a negative amount is rejected`() {
        assertThrows(RtsGenericException::class.java) {
            contactOpeningBalanceService.set(contactId, setDto(payable = "0", receivable = "-1.00"))
        }
        assertTrue(savedRevisions.isEmpty())
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
    fun `history lists each claimed amount with its direction, balance signal and author`() {
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
            listOf(BalanceSignal.MONEY_IN, BalanceSignal.MONEY_OUT, BalanceSignal.ZERO_BALANCE),
            history.map { it.balanceSignal }
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
        payable = payable?.let { Optional.of(BigDecimal(it)) },
        receivable = receivable?.let { Optional.of(BigDecimal(it)) }
    )

    private fun publishedSignedDeltas(): List<String> =
        mockingDetails(contactOpeningBalanceHandlerForKafka).invocations
            .filter { it.method.name == "publish" }
            .map { (it.arguments[1] as BigDecimal).toPlainString() }

    private fun lockInvocations() =
        mockingDetails(orgEntityAdvisoryLock).invocations.filter { it.method.name == "acquire" }
}
