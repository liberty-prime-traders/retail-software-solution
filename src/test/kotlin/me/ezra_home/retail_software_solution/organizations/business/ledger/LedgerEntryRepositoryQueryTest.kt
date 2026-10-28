package me.ezra_home.retail_software_solution.organizations.business.ledger

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.data.jpa.repository.Query

class LedgerEntryRepositoryQueryTest {

    private val locationPredicate = "AND g.source_location_id = :locationId"

    @Test
    fun `the location movements query is the organization query plus the location predicate and nothing else`() {
        val organizationQuery = queryOf("sumMovementsInRange")
        val locationQuery = queryOf("sumMovementsInRangeForLocation")

        assertFalse(organizationQuery.contains(locationPredicate))
        assertTrue(locationQuery.contains(locationPredicate))
        assertEquals(organizationQuery.normalizedSpacing(), locationQuery.replace(locationPredicate, "").normalizedSpacing())
    }

    private fun queryOf(methodName: String): String =
        LedgerEntryRepository::class.java.declaredMethods.single { it.name == methodName }.getAnnotation(Query::class.java).value

    private fun String.normalizedSpacing(): String = trim().replace(Regex("\\s+"), " ")
}
