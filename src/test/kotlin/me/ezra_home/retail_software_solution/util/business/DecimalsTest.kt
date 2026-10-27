package me.ezra_home.retail_software_solution.util.business

import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode

class DecimalsTest {

    @Test
    fun `values equal at scale 4 compare equal whatever their stored scale`() {
        assertEquals(0, Decimals.Scale4.compare(BigDecimal("2080"), BigDecimal("2080.0000")))
        assertEquals(0, Decimals.Scale4.compare(BigDecimal("25.99996"), BigDecimal("26.0000")))
    }

    @Test
    fun `values that differ at scale 4 are ordered`() {
        assertTrue(Decimals.Scale4.compare(BigDecimal("25.9992"), BigDecimal("26")) < 0)
        assertTrue(Decimals.Scale4.compare(BigDecimal("26.0001"), BigDecimal("26")) > 0)
    }

    @Test
    fun `each scale compares at its own precision`() {
        assertEquals(0, Decimals.MoneyScale.compare(BigDecimal("25.9992"), BigDecimal("26")))
        assertTrue(Decimals.Scale4.compare(BigDecimal("25.9992"), BigDecimal("26")) < 0)
    }

    @Test
    fun `stripping zeroes rounds half up`() {
        assertEquals("1", Decimals.stripZeroesAndRound(BigDecimal("0.99995")))
    }

    @Test
    fun `shown on hand minus shown reserved never exceeds shown available`() {
        listOf(
            "1.00019" to "0.00006",
            "1.00011" to "0.00006",
            "1.00004" to "0.00003",
            "0.99995" to "0.00005",
            "12.34567" to "2.00001",
        ).forEach { (onHand, reserved) ->
            val onHandExact = BigDecimal(onHand)
            val reservedExact = BigDecimal(reserved)
            val shownOnHand = onHandExact.setScale(Decimals.Scale4.scale, RoundingMode.DOWN)
            val shownReserved = reservedExact.setScale(Decimals.Scale4.scale, RoundingMode.UP)
            val shownAvailable = (onHandExact - reservedExact).setScale(Decimals.Scale4.scale, RoundingMode.DOWN)
            assertTrue(shownOnHand - shownReserved <= shownAvailable, "$onHand on hand, $reserved reserved")
        }
    }

    @Test
    fun `zero and negative values are not positive transaction amounts`() {
        listOf("0", "-5").forEach { amount ->
            assertThrows(RtsGenericException::class.java) { Decimals.requirePositive(BigDecimal(amount), "Amount") }
        }
    }

    @Test
    fun `a magnitude may equal the transaction maximum in either sign but not exceed it`() {
        Decimals.requireWithinMaximum(Decimals.MAXIMUM_TRANSACTION_AMOUNT, Decimals.MAXIMUM_TRANSACTION_AMOUNT, "Amount")
        Decimals.requireWithinMaximum(Decimals.MAXIMUM_TRANSACTION_AMOUNT.negate(), Decimals.MAXIMUM_TRANSACTION_AMOUNT, "Amount")
        val justOverMaximum = Decimals.MAXIMUM_TRANSACTION_AMOUNT.add(BigDecimal("0.01"))
        listOf(justOverMaximum, justOverMaximum.negate()).forEach { overMaximum ->
            assertThrows(RtsGenericException::class.java) {
                Decimals.requireWithinMaximum(overMaximum, Decimals.MAXIMUM_TRANSACTION_AMOUNT, "Amount")
            }
        }
    }

    @Test
    fun `money rounds half up to two decimals`() {
        assertEquals(2, Decimals.MoneyScale.scale)
        assertEquals(BigDecimal("6115.63"), Decimals.MoneyScale.round(BigDecimal("6115.625")))
    }

    @Test
    fun `the label names the field in the positivity error`() {
        val exception = assertThrows(RtsGenericException::class.java) { Decimals.requirePositive(BigDecimal.ZERO, "Payment amount") }
        assertEquals("Payment amount must be greater than zero", exception.message)
    }
}
