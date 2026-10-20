package me.ezra_home.retail_software_solution.organizations.business.loan

import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanSearchParameters
import me.ezra_home.retail_software_solution.organizations.business.loan.api.LoanStatus
import me.ezra_home.retail_software_solution.organizations.business.loan.search.LoanSearchParameterNames
import me.ezra_home.retail_software_solution.organizations.business.loan.search.LoanSearchQueryBuilder
import me.ezra_home.retail_software_solution.organizations.business.loan.search.LoanSearchValidator
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import me.ezra_home.retail_software_solution.util.queries.QueryParameterNames
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class LoanSearchTest {

    private val createdFrom = OffsetDateTime.parse("2026-01-01T00:00:00Z")

    private fun bounded(parameters: LoanSearchParameters = LoanSearchParameters()) =
        parameters.copy(createdFrom = createdFrom, createdBefore = createdFrom.plusMonths(1))

    @Test
    fun `both created bounds are required and may span at most one year`() {
        assertThrows(RtsGenericException::class.java) {
            LoanSearchValidator.guardValidParameters(LoanSearchParameters(createdFrom = createdFrom))
        }
        assertThrows(RtsGenericException::class.java) {
            LoanSearchValidator.guardValidParameters(LoanSearchParameters(createdBefore = createdFrom))
        }
        assertThrows(RtsGenericException::class.java) {
            LoanSearchValidator.guardValidParameters(
                LoanSearchParameters(createdFrom = createdFrom, createdBefore = createdFrom.plusYears(1).plusDays(1))
            )
        }
        LoanSearchValidator.guardValidParameters(bounded())
    }

    @Test
    fun `loan date bounds must be in order`() {
        assertThrows(RtsGenericException::class.java) {
            LoanSearchValidator.guardValidParameters(
                bounded(LoanSearchParameters(loanDateFrom = LocalDate.of(2026, 3, 2), loanDateBefore = LocalDate.of(2026, 3, 1)))
            )
        }
    }

    @Test
    fun `the lower bound of the created range is inclusive and the upper exclusive`() {
        val predicate = LoanSearchQueryBuilder.buildPredicate(bounded())

        assertTrue(predicate.whereClauses.contains("l.created_on >= :createdFrom"))
        assertTrue(predicate.whereClauses.contains("l.created_on < :createdBefore"))
    }

    @Test
    fun `lenders filter on the contact's reference number`() {
        val predicate = LoanSearchQueryBuilder.buildPredicate(bounded(LoanSearchParameters(lenderContactReferences = listOf("CT000001"))))

        assertTrue(predicate.whereClauses.contains("c.reference_number = ANY(:${LoanSearchParameterNames.LENDER_CONTACT_REFERENCES})"))
        assertEquals(listOf("CT000001"), (predicate.params.getValue(LoanSearchParameterNames.LENDER_CONTACT_REFERENCES) as Array<*>).toList())
    }

    @Test
    fun `a status filter matches the loan_status codes, and all or no statuses filter nothing`() {
        fun statusClauses(statuses: Set<LoanStatus>) = LoanSearchQueryBuilder
            .buildPredicate(bounded(LoanSearchParameters(statuses = statuses)))

        val openOrSettled = statusClauses(setOf(LoanStatus.OPEN, LoanStatus.SETTLED))
        assertTrue(openOrSettled.whereClauses.contains("ls.status = ANY(:${LoanSearchParameterNames.STATUSES})"))
        assertEquals(setOf("OPN", "STL"), (openOrSettled.params.getValue(LoanSearchParameterNames.STATUSES) as Array<*>).toSet())

        assertTrue(statusClauses(LoanStatus.entries.toSet()).whereClauses.none { it.contains("ls.status") })
        assertTrue(statusClauses(emptySet()).whereClauses.none { it.contains("ls.status") })
    }

    @Test
    fun `blank references are dropped before they reach the query`() {
        val sanitized = LoanSearchParameters(references = listOf("LOAN000001", " ", ""), lenderContactReferences = listOf("", "CT000001")).sanitized()

        assertEquals(listOf("LOAN000001"), sanitized.references)
        assertEquals(listOf("CT000001"), sanitized.lenderContactReferences)
    }

    @Test
    fun `the list query returns only ids and pages by created_on and id descending with an exclusive cursor`() {
        val cursor = KeysetSearchCursor(createdFrom, UUID.randomUUID())
        val sqlQuery = LoanSearchQueryBuilder.buildListQuery(LoanSearchQueryBuilder.buildPredicate(bounded()), cursor)

        assertTrue(sqlQuery.sql.contains("l.id AS id") && sqlQuery.sql.contains("l.created_on AS created_on"))
        assertFalse(sqlQuery.sql.contains("l.amount AS"))
        assertTrue(sqlQuery.sql.contains("ORDER BY l.created_on DESC, l.id DESC"))
        assertTrue(sqlQuery.sql.contains("(l.created_on, l.id) < (:${QueryParameterNames.CURSOR_CREATED_ON}, :${QueryParameterNames.CURSOR_ID})"))
    }
}
