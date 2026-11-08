package me.ezra_home.retail_software_solution.organizations.business.ledger.api

import me.ezra_home.retail_software_solution.configuration.session.LocationSession
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryEntity
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryGroupEntity
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryGroupRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryRepository
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class PostedStockAdjustmentAccountsReaderTest {

    private val locationId = UUID.randomUUID()
    private val ledgerEntryGroupRepository = mock(LedgerEntryGroupRepository::class.java)
    private val ledgerEntryRepository = mock(LedgerEntryRepository::class.java)
    private val postedStockAdjustmentAccountsReader = PostedStockAdjustmentAccountsReader(
        ledgerEntryGroupRepository, ledgerEntryRepository
    )

    @BeforeEach
    fun setSession() {
        SessionContextProvider.setSession(
            SessionContext(
                organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"),
                location = LocationSession(id = locationId, schemaName = "loc-1")
            )
        )
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    private fun givenGroups(vararg adjustmentAndGroupReferences: Pair<String, String>) {
        val adjustmentReferences = adjustmentAndGroupReferences.map { it.first }.toSet()
        `when`(
            ledgerEntryGroupRepository.findBySourceReferenceNumberInAndSourceTypeAndSourceLocationId(
                adjustmentReferences, LedgerSourceType.STOCK_ADJUSTMENT, locationId
            )
        ).thenReturn(
            adjustmentAndGroupReferences.map { (adjustmentReference, groupReference) ->
                LedgerEntryGroupEntity(
                    sourceReferenceNumber = adjustmentReference,
                    sourceType = LedgerSourceType.STOCK_ADJUSTMENT,
                    sourceLocationId = locationId,
                    fiscalPeriodId = UUID.randomUUID(),
                    postedOn = Instant.now()
                ).also { it.referenceNumber = groupReference }
            }
        )
    }

    private fun givenEntries(groupReferences: Set<String>, vararg entries: Triple<String, String, EntryType>) {
        `when`(ledgerEntryRepository.findByGroupReferenceNumberIn(groupReferences)).thenReturn(
            entries.map { (groupReference, accountCode, entryType) ->
                LedgerEntryEntity(groupReference, accountCode, entryType, BigDecimal("10.00"))
            }
        )
    }

    @Test
    fun `a posted adjustment returns the accounts it debited and credited whatever the entry order`() {
        givenGroups("ADJ1" to "LEG1")
        givenEntries(
            setOf("LEG1"),
            Triple("LEG1", "INVENTORY", EntryType.CREDIT),
            Triple("LEG1", "EXPENSE", EntryType.DEBIT),
        )

        assertEquals(
            mapOf("ADJ1" to PostedStockAdjustmentAccounts("EXPENSE", "INVENTORY")),
            postedStockAdjustmentAccountsReader.getByAdjustmentReferenceNumber(setOf("ADJ1"))
        )
    }

    @Test
    fun `each adjustment gets the accounts of its own group`() {
        givenGroups("ADJ1" to "LEG1", "ADJ2" to "LEG2")
        givenEntries(
            setOf("LEG1", "LEG2"),
            Triple("LEG1", "EXPENSE", EntryType.DEBIT),
            Triple("LEG1", "INVENTORY", EntryType.CREDIT),
            Triple("LEG2", "SHRINKAGE", EntryType.DEBIT),
            Triple("LEG2", "INVENTORY", EntryType.CREDIT),
        )

        val accountsByAdjustment = postedStockAdjustmentAccountsReader
            .getByAdjustmentReferenceNumber(setOf("ADJ1", "ADJ2"))

        assertEquals("EXPENSE", accountsByAdjustment.getValue("ADJ1").debitAccountCode)
        assertEquals("SHRINKAGE", accountsByAdjustment.getValue("ADJ2").debitAccountCode)
    }

    @Test
    fun `a posting with a group but no entries is a cost zero adjustment and is left out of the result`() {
        givenGroups("ADJ1" to "LEG1")
        `when`(ledgerEntryRepository.findByGroupReferenceNumberIn(setOf("LEG1"))).thenReturn(emptyList())

        assertTrue(postedStockAdjustmentAccountsReader.getByAdjustmentReferenceNumber(setOf("ADJ1")).isEmpty())
    }

    @Test
    fun `a posting waits for an adjustment that has no group yet`() {
        `when`(
            ledgerEntryGroupRepository.findBySourceReferenceNumberInAndSourceTypeAndSourceLocationId(
                setOf("ADJ1", "ADJ2"), LedgerSourceType.STOCK_ADJUSTMENT, locationId
            )
        ).thenReturn(
            listOf(
                LedgerEntryGroupEntity(
                    "ADJ1", LedgerSourceType.STOCK_ADJUSTMENT, locationId, UUID.randomUUID(), Instant.now()
                ).also { it.referenceNumber = "LEG1" }
            )
        )

        val exception = assertThrows(RtsGenericException::class.java) {
            postedStockAdjustmentAccountsReader.getByAdjustmentReferenceNumber(setOf("ADJ1", "ADJ2"))
        }

        assertTrue(exception.message.contains("ADJ2"))
    }

    @Test
    fun `a preview skips an adjustment that has no group yet instead of failing`() {
        `when`(
            ledgerEntryGroupRepository.findBySourceReferenceNumberInAndSourceTypeAndSourceLocationId(
                setOf("ADJ1"), LedgerSourceType.STOCK_ADJUSTMENT, locationId
            )
        ).thenReturn(emptyList())

        assertTrue(postedStockAdjustmentAccountsReader.findByAdjustmentReferenceNumber(setOf("ADJ1")).isEmpty())
    }
}
