package me.ezra_home.retail_software_solution.util.business

import me.ezra_home.retail_software_solution.support.WithDefaultOrganizationSession
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

@WithDefaultOrganizationSession
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
        assertEquals(0, Decimals.compareAtScale(BigDecimal("25.9992"), BigDecimal("26"), Decimals.MoneyScale))
        assertTrue(Decimals.compareAtScale(BigDecimal("25.9992"), BigDecimal("26"), Decimals.Scale4) < 0)
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
        listOf(Decimals.MAXIMUM_TRANSACTION_AMOUNT, Decimals.MAXIMUM_TRANSACTION_AMOUNT.negate()).forEach { atMaximum ->
            assertThrows(RtsGenericException::class.java) {
                Decimals.requireWithinMaximum(atMaximum.add(atMaximum.signum().toBigDecimal().multiply(BigDecimal("0.01"))), Decimals.MAXIMUM_TRANSACTION_AMOUNT, "Amount")
            }
        }
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `money rounds to two decimals when the session carries no organization`() {
        assertEquals(2, Decimals.MoneyScale.scale)
        assertEquals(BigDecimal("6115.63"), Decimals.MoneyScale.round(BigDecimal("6115.625")))
    }

    @Test
    fun `money follows the organization scale in the session, resolved on every call`() {
        val zeroDecimalOrganization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", moneyScale = 0)
        val threeDecimalOrganization = OrgSession(id = UUID.randomUUID(), schemaName = "org-b", moneyScale = 3)

        SessionContextProvider.setSession(SessionContext(organization = zeroDecimalOrganization))
        assertEquals(BigDecimal("6116"), Decimals.MoneyScale.round(BigDecimal("6115.625")))

        SessionContextProvider.setSession(SessionContext(organization = threeDecimalOrganization))
        assertEquals(BigDecimal("6115.625"), Decimals.MoneyScale.round(BigDecimal("6115.625")))
    }

    @Test
    fun `the label names the field in the positivity error`() {
        val exception = assertThrows(RtsGenericException::class.java) { Decimals.requirePositive(BigDecimal.ZERO, "Payment amount") }
        assertEquals("Payment amount must be greater than zero", exception.message)
    }
}
