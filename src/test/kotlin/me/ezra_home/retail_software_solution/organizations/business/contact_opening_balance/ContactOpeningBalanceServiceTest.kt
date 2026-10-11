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
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
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
import java.util.UUID

/** Revisions are append-only deltas; a wrong delta, a skipped lock or a missing guard corrupts the control accounts. */
class ContactOpeningBalanceServiceTest {

    private val latestRevisionsBySubledgerDirection = mutableMapOf<ContactSubledgerDirection, ContactOpeningBalanceEntity>()
    private val savedRevisions = mutableListOf<ContactOpeningBalanceEntity>()
    private val contactOpeningBalanceRepository = Proxy.newProxyInstance(
        ContactOpeningBalanceRepository::class.java.classLoader,
        arrayOf(ContactOpeningBalanceRepository::class.java)
    ) { _, method, arguments ->
        when (method.name) {
            "save" -> (arguments[0] as ContactOpeningBalanceEntity).also { savedRevisions.add(it) }
            "findFirstByContactReferenceNumberAndSubledgerDirectionOrderByCreatedOnDesc" -> latestRevisionsBySubledgerDirection[arguments[1]]
            else -> throw UnsupportedOperationException(method.name)
        }
    } as ContactOpeningBalanceRepository

    private val contactService = mock(ContactService::class.java)
    private val contactOpeningBalanceHandlerForKafka = mock(ContactOpeningBalanceHandlerForKafka::class.java)
    private val orgEntityAdvisoryLock = mock(OrgEntityAdvisoryLock::class.java)
    private val contactOpeningBalanceService = ContactOpeningBalanceService(
        contactOpeningBalanceRepository,
        contactService,
        contactOpeningBalanceHandlerForKafka,
        orgEntityAdvisoryLock,
        mock(FiscalPeriodService::class.java)
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
        contactOpeningBalanceService.set(contactId, setDto(ContactSubledgerDirection.RECEIVABLE, "100.00"))

        val saved = savedRevisions.single()
        assertEquals(ContactSubledgerDirection.RECEIVABLE, saved.subledgerDirection)
        assertEquals(listOf("100.0000"), publishedSignedDeltas())
        assertTrue(lockInvocations().single().arguments.toList() == listOf(LockNamespaces.CONTACT_OPENING_BALANCE, contactReferenceNumber))
    }

    @Test
    fun `a revision upward and downward publishes the signed delta against the previous revision`() {
        givenLatestRevision(revisionRow(ContactSubledgerDirection.PAYABLE, "100.00"))
        contactOpeningBalanceService.set(contactId, setDto(ContactSubledgerDirection.PAYABLE, "130.00"))
        givenLatestRevision(savedRevisions.last())
        contactOpeningBalanceService.set(contactId, setDto(ContactSubledgerDirection.PAYABLE, "90.00"))

        assertEquals(2, savedRevisions.size)
        assertEquals(listOf("30.0000", "-40.0000"), publishedSignedDeltas())
    }

    @Test
    fun `a zero delta records no revision and publishes nothing`() {
        givenLatestRevision(revisionRow(ContactSubledgerDirection.RECEIVABLE, "100.00"))

        contactOpeningBalanceService.set(contactId, setDto(ContactSubledgerDirection.RECEIVABLE, "100.00"))

        assertTrue(savedRevisions.isEmpty())
        assertTrue(publishedSignedDeltas().isEmpty())
    }

    @Test
    fun `an amount beyond scale 4 is compared and stored at scale 4`() {
        givenLatestRevision(revisionRow(ContactSubledgerDirection.RECEIVABLE, "100.0001"))

        contactOpeningBalanceService.set(contactId, setDto(ContactSubledgerDirection.RECEIVABLE, "100.00005"))

        assertTrue(savedRevisions.isEmpty())
        assertTrue(publishedSignedDeltas().isEmpty())

        contactOpeningBalanceService.set(contactId, setDto(ContactSubledgerDirection.PAYABLE, "10.00005"))

        assertEquals("10.0001", savedRevisions.single().amount.toPlainString())
        assertEquals(listOf("10.0001"), publishedSignedDeltas())
    }

    @Test
    fun `each subledger direction is revised against its own latest revision`() {
        givenLatestRevision(revisionRow(ContactSubledgerDirection.RECEIVABLE, "100.00"))

        contactOpeningBalanceService.set(contactId, setDto(ContactSubledgerDirection.PAYABLE, "40.00"))

        assertEquals(ContactSubledgerDirection.PAYABLE, savedRevisions.single().subledgerDirection)
        assertEquals(listOf("40.0000"), publishedSignedDeltas())
    }

    @Test
    fun `a negative amount is rejected`() {
        assertThrows(RtsGenericException::class.java) {
            contactOpeningBalanceService.set(contactId, setDto(ContactSubledgerDirection.RECEIVABLE, "-1.00"))
        }
        assertTrue(savedRevisions.isEmpty())
    }

    @Test
    fun `an inactive contact is rejected`() {
        givenContact(ContactStatus.INACTIVE)

        assertThrows(RtsGenericException::class.java) {
            contactOpeningBalanceService.set(contactId, setDto(ContactSubledgerDirection.RECEIVABLE, "10.00"))
        }
        assertTrue(savedRevisions.isEmpty())
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

    private fun setDto(subledgerDirection: ContactSubledgerDirection, amount: String) =
        ContactOpeningBalanceSetDto(subledgerDirection, BigDecimal(amount))

    private fun publishedSignedDeltas(): List<String> =
        mockingDetails(contactOpeningBalanceHandlerForKafka).invocations
            .filter { it.method.name == "publish" }
            .map { (it.arguments[1] as BigDecimal).toPlainString() }

    private fun lockInvocations() =
        mockingDetails(orgEntityAdvisoryLock).invocations.filter { it.method.name == "acquire" }
}
