package me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance

import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactOpeningBalanceReader
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactSubledgerDirection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy
import java.math.BigDecimal

/** A contact with only one subledgerDirection declared must still read the other side as zero, or the net flips sign. */
class ContactOpeningBalanceReaderTest {

    private val latestRevisions = listOf(
        revision("CT000001", ContactSubledgerDirection.RECEIVABLE, "100.00"),
        revision("CT000001", ContactSubledgerDirection.PAYABLE, "40.00"),
        revision("CT000002", ContactSubledgerDirection.PAYABLE, "25.00")
    )
    private val contactOpeningBalanceRepository = Proxy.newProxyInstance(
        ContactOpeningBalanceRepository::class.java.classLoader,
        arrayOf(ContactOpeningBalanceRepository::class.java)
    ) { _, method, _ ->
        if (method.name == "findLatestForContacts") latestRevisions else throw UnsupportedOperationException(method.name)
    } as ContactOpeningBalanceRepository
    private val contactOpeningBalanceReader = ContactOpeningBalanceReader(contactOpeningBalanceRepository)

    @Test
    fun `each contact gets its own receivable and payable opening amounts`() {
        val openingBalances = contactOpeningBalanceReader.getOpeningBalances(setOf("CT000001", "CT000002", "CT000003"))

        assertEquals(0, BigDecimal("100.00").compareTo(openingBalances.getValue("CT000001").receivable))
        assertEquals(0, BigDecimal("40.00").compareTo(openingBalances.getValue("CT000001").payable))
        assertEquals(0, BigDecimal.ZERO.compareTo(openingBalances.getValue("CT000002").receivable))
        assertEquals(0, BigDecimal("25.00").compareTo(openingBalances.getValue("CT000002").payable))
        assertTrue("CT000003" !in openingBalances)
    }

    @Test
    fun `no contacts means no query`() {
        assertTrue(contactOpeningBalanceReader.getOpeningBalances(emptySet()).isEmpty())
    }

    private fun revision(contactReferenceNumber: String, subledgerDirection: ContactSubledgerDirection, amount: String) =
        ContactOpeningBalanceEntity(contactReferenceNumber, subledgerDirection, BigDecimal(amount))
}
