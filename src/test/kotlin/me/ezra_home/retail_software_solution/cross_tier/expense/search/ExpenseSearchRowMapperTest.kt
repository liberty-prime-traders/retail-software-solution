package me.ezra_home.retail_software_solution.cross_tier.expense.search

import jakarta.persistence.Tuple
import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

class ExpenseSearchRowMapperTest {

    @Test
    fun `a list tuple carries the keys needed to page and load the expense`() {
        val expenseId = UUID.randomUUID()
        val tuple = mock(Tuple::class.java)
        `when`(tuple.get("id", UUID::class.java)).thenReturn(expenseId)
        `when`(tuple.get("reference_number", String::class.java)).thenReturn("EXPN000001")
        `when`(tuple.get("created_on", Instant::class.java)).thenReturn(Instant.parse("2030-01-01T00:00:00Z"))

        val row = ExpenseSearchRowMapper.fromTuple(tuple)

        assertEquals(expenseId, row.id)
        assertEquals("EXPN000001", row.referenceNumber)
        assertEquals(OffsetDateTime.parse("2030-01-01T00:00:00Z"), row.createdOn)
    }

    @Test
    fun `a voided expense lands in the voided bucket whatever its stored status`() {
        PaymentStatus.entries.forEach {
            assertEquals(ExpenseSummaryBucket.VOIDED, ExpenseSearchMapper.toBucket(voided = true, paymentStatus = it), "status $it")
        }
    }

    @Test
    fun `a live expense lands in the bucket for its stored status`() {
        assertEquals(ExpenseSummaryBucket.UNPAID, ExpenseSearchMapper.toBucket(false, PaymentStatus.UNPAID))
        assertEquals(ExpenseSummaryBucket.PARTIAL, ExpenseSearchMapper.toBucket(false, PaymentStatus.PARTIALLY_SETTLED))
        assertEquals(ExpenseSummaryBucket.PAID, ExpenseSearchMapper.toBucket(false, PaymentStatus.FULLY_SETTLED))
    }
}
