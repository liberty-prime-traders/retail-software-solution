package me.ezra_home.retail_software_solution.util.business

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

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
    fun `the scale overload compares at the scale it is given`() {
        assertEquals(0, Decimals.compareAtScale(BigDecimal("25.9992"), BigDecimal("26"), 2))
        assertTrue(Decimals.compareAtScale(BigDecimal("25.9992"), BigDecimal("26"), 4) < 0)
    }

    @Test
    fun `rounding down never reports more than is there`() {
        assertEquals(BigDecimal("0.9999"), Decimals.Scale4.roundDown(BigDecimal("0.99995")))
        assertEquals("0.9999", Decimals.stripZeroesAndRoundDown(BigDecimal("0.99995")))
        assertEquals("1", Decimals.stripZeroesAndRound(BigDecimal("0.99995")))
    }

    @Test
    fun `rounding up never reports less than is held`() {
        assertEquals(BigDecimal("0.0001"), Decimals.Scale4.roundUp(BigDecimal("0.00006")))
        assertEquals(BigDecimal("0.0001"), Decimals.Scale4.roundUp(BigDecimal("0.00001")))
        assertEquals(BigDecimal("0.0000"), Decimals.Scale4.roundUp(BigDecimal.ZERO))
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
            val shownOnHand = Decimals.Scale4.roundDown(onHandExact)
            val shownReserved = Decimals.Scale4.roundUp(reservedExact)
            val shownAvailable = Decimals.Scale4.roundDown(onHandExact - reservedExact)
            assertTrue(shownOnHand - shownReserved <= shownAvailable, "$onHand on hand, $reserved reserved")
        }
    }
}
