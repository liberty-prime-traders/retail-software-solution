package me.ezra_home.retail_software_solution.organizations.business.fund_movement

import me.ezra_home.retail_software_solution.organizations.business.fund_movement.api.FundMovementSearchParameters
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.api.FundMovementStatus
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.api.FundMovementType
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.search.FundMovementSearchParameterNames
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.search.FundMovementSearchQueryBuilder
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.search.FundMovementSearchValidator
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import me.ezra_home.retail_software_solution.util.queries.QueryParameterNames
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID

class FundMovementSearchTest {

    private val createdFrom = OffsetDateTime.parse("2026-01-01T00:00:00Z")

    private fun bounded(parameters: FundMovementSearchParameters = FundMovementSearchParameters()) =
        parameters.copy(createdFrom = createdFrom, createdBefore = createdFrom.plusMonths(1))

    @Test
    fun `both created bounds are required and may span at most one year`() {
        assertThrows(RtsGenericException::class.java) {
            FundMovementSearchValidator.guardValidParameters(FundMovementSearchParameters(createdFrom = createdFrom))
        }
        assertThrows(RtsGenericException::class.java) {
            FundMovementSearchValidator.guardValidParameters(
                FundMovementSearchParameters(createdFrom = createdFrom, createdBefore = createdFrom.plusYears(1).plusDays(1))
            )
        }
        FundMovementSearchValidator.guardValidParameters(bounded())
    }

    @Test
    fun `the lower bound of the created range is inclusive and the upper exclusive`() {
        val predicate = FundMovementSearchQueryBuilder.buildPredicate(bounded())

        assertTrue(predicate.whereClauses.contains("fm.created_on >= :createdFrom"))
        assertTrue(predicate.whereClauses.contains("fm.created_on < :createdBefore"))
    }

    @Test
    fun `an account filter matches either side of the movement in a single-row predicate`() {
        val predicate = FundMovementSearchQueryBuilder.buildPredicate(bounded(FundMovementSearchParameters(accountCodes = listOf("001.001.001"))))

        val accountClause = predicate.whereClauses.single { it.contains("debit_account_code") }
        assertTrue(accountClause.contains("fm.credit_account_code = ANY(:${FundMovementSearchParameterNames.ACCOUNT_CODES})"))
        assertTrue(predicate.params.containsKey(FundMovementSearchParameterNames.ACCOUNT_CODES))
    }

    @Test
    fun `types filter on the stored codes`() {
        val predicate = FundMovementSearchQueryBuilder.buildPredicate(
            bounded(FundMovementSearchParameters(types = setOf(FundMovementType.WITHDRAW_FOR_PERSONAL_USE)))
        )

        assertEquals(listOf(FundMovementType.WITHDRAW_FOR_PERSONAL_USE.code), (predicate.params.getValue(FundMovementSearchParameterNames.TYPES) as Array<*>).toList())
    }

    @Test
    fun `a single status filters on the void row and both statuses filter nothing`() {
        fun voidClauses(statuses: Set<FundMovementStatus>) = FundMovementSearchQueryBuilder
            .buildPredicate(bounded(FundMovementSearchParameters(statuses = statuses))).whereClauses.filter { it.contains("fmv.id") }

        assertEquals(listOf("fmv.id IS NOT NULL"), voidClauses(setOf(FundMovementStatus.VOIDED)))
        assertEquals(listOf("fmv.id IS NULL"), voidClauses(setOf(FundMovementStatus.ACTIVE)))
        assertTrue(voidClauses(FundMovementStatus.entries.toSet()).isEmpty())
        assertTrue(voidClauses(emptySet()).isEmpty())
    }

    @Test
    fun `blank references and account codes are dropped before they reach the query`() {
        val sanitized = FundMovementSearchParameters(references = listOf("FMOV000001", " ", ""), accountCodes = listOf("", "001.001.001")).sanitized()

        assertEquals(listOf("FMOV000001"), sanitized.references)
        assertEquals(listOf("001.001.001"), sanitized.accountCodes)
    }

    @Test
    fun `the list query pages by created_on and id descending with the cursor as an exclusive bound`() {
        val cursor = KeysetSearchCursor(createdFrom, UUID.randomUUID())
        val sqlQuery = FundMovementSearchQueryBuilder.buildListQuery(FundMovementSearchQueryBuilder.buildPredicate(bounded()), cursor)

        assertTrue(sqlQuery.sql.contains("ORDER BY fm.created_on DESC, fm.id DESC"))
        assertTrue(sqlQuery.sql.contains("(fm.created_on, fm.id) < (:${QueryParameterNames.CURSOR_CREATED_ON}, :${QueryParameterNames.CURSOR_ID})"))
        assertFalse(sqlQuery.sql.contains("JOIN fund_movement_void fmv ON fmv.fund_movement_id = fm.id JOIN"))
    }
}
