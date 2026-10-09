package me.ezra_home.retail_software_solution.cross_tier.expense.search

import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class ExpenseSearchValidatorTest {

    private val createdFrom = OffsetDateTime.parse("2030-01-01T00:00:00Z")
    private val createdBefore = OffsetDateTime.parse("2030-02-01T00:00:00Z")

    @Test
    fun `parameters with only the created range are valid`() {
        assertDoesNotThrow { ExpenseSearchValidator.guardValidParameters(rangedParameters()) }
    }

    @Test
    fun `both created bounds are required`() {
        assertRejected(ExpenseSearchParameters())
        assertRejected(ExpenseSearchParameters(createdFrom = createdFrom))
        assertRejected(ExpenseSearchParameters(createdBefore = createdBefore))
    }

    @Test
    fun `the created range may span at most one year`() {
        assertDoesNotThrow {
            ExpenseSearchValidator.guardValidParameters(rangedParameters(createdBefore = createdFrom.plusYears(1)))
        }
        assertRejected(rangedParameters(createdBefore = createdFrom.plusYears(1).plusSeconds(1)))
    }

    @Test
    fun `reference lists are capped at 20`() {
        val twenty = List(20) { "REF$it" }
        val twentyOne = twenty + "REF20"

        assertDoesNotThrow { ExpenseSearchValidator.guardValidParameters(rangedParameters(expenseReferenceNumbers = twenty)) }
        assertDoesNotThrow { ExpenseSearchValidator.guardValidParameters(rangedParameters(sourceReferences = twenty)) }
        assertRejected(rangedParameters(expenseReferenceNumbers = twentyOne))
        assertRejected(rangedParameters(sourceReferences = twentyOne))
    }

    @Test
    fun `id lists are capped at 100`() {
        val hundred = List(100) { UUID.randomUUID() }
        val hundredAndOne = hundred + UUID.randomUUID()

        assertDoesNotThrow {
            ExpenseSearchValidator.guardValidParameters(
                rangedParameters(payeeContactIds = hundred, expenseTypeIds = hundred, paymentMethodIds = hundred)
            )
        }
        assertRejected(rangedParameters(payeeContactIds = hundredAndOne))
        assertRejected(rangedParameters(expenseTypeIds = hundredAndOne))
        assertRejected(rangedParameters(paymentMethodIds = hundredAndOne))
    }

    @Test
    fun `created range must not be reversed but may be a single instant`() {
        assertDoesNotThrow {
            ExpenseSearchValidator.guardValidParameters(ExpenseSearchParameters(createdFrom = createdFrom, createdBefore = createdFrom))
        }
        assertRejected(ExpenseSearchParameters(createdFrom = createdFrom.plusSeconds(1), createdBefore = createdFrom))
    }

    @Test
    fun `expense date range must not be reversed but may be a single day`() {
        val day = LocalDate.of(2030, 1, 1)

        assertDoesNotThrow { ExpenseSearchValidator.guardValidParameters(rangedParameters(expenseDateFrom = day, expenseDateBefore = day)) }
        assertRejected(rangedParameters(expenseDateFrom = day.plusDays(1), expenseDateBefore = day))
    }

    @Test
    fun `amount range must not be reversed but may be a single value`() {
        assertDoesNotThrow {
            ExpenseSearchValidator.guardValidParameters(rangedParameters(minAmount = BigDecimal.TEN, maxAmount = BigDecimal.TEN))
        }
        assertRejected(rangedParameters(minAmount = BigDecimal("10.01"), maxAmount = BigDecimal.TEN))
    }

    @Test
    fun `amount bounds may be zero but not negative`() {
        assertDoesNotThrow {
            ExpenseSearchValidator.guardValidParameters(rangedParameters(minAmount = BigDecimal.ZERO, maxAmount = BigDecimal.ZERO))
        }
        assertRejected(rangedParameters(minAmount = BigDecimal("-0.01")))
        assertRejected(rangedParameters(maxAmount = BigDecimal("-1")))
    }

    @Test
    fun `page size must be between 1 and 500`() {
        assertDoesNotThrow { ExpenseSearchValidator.guardValidPageSize(1) }
        assertDoesNotThrow { ExpenseSearchValidator.guardValidPageSize(500) }
        assertThrows(RtsGenericException::class.java) { ExpenseSearchValidator.guardValidPageSize(0) }
        assertThrows(RtsGenericException::class.java) { ExpenseSearchValidator.guardValidPageSize(501) }
    }

    private fun rangedParameters(
        expenseReferenceNumbers: List<String> = emptyList(),
        sourceReferences: List<String> = emptyList(),
        payeeContactIds: List<UUID> = emptyList(),
        expenseTypeIds: List<UUID> = emptyList(),
        paymentMethodIds: List<UUID> = emptyList(),
        expenseDateFrom: LocalDate? = null,
        expenseDateBefore: LocalDate? = null,
        minAmount: BigDecimal? = null,
        maxAmount: BigDecimal? = null,
        createdBefore: OffsetDateTime = this.createdBefore
    ) = ExpenseSearchParameters(
        expenseReferenceNumbers = expenseReferenceNumbers,
        sourceReferences = sourceReferences,
        payeeContactIds = payeeContactIds,
        expenseTypeIds = expenseTypeIds,
        paymentMethodIds = paymentMethodIds,
        createdFrom = createdFrom,
        createdBefore = createdBefore,
        expenseDateFrom = expenseDateFrom,
        expenseDateBefore = expenseDateBefore,
        minAmount = minAmount,
        maxAmount = maxAmount
    )

    private fun assertRejected(expenseSearchParameters: ExpenseSearchParameters) {
        assertThrows(RtsGenericException::class.java) { ExpenseSearchValidator.guardValidParameters(expenseSearchParameters) }
    }
}
