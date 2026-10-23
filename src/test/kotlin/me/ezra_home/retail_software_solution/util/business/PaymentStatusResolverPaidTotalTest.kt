package me.ezra_home.retail_software_solution.util.business

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class PaymentStatusResolverPaidTotalTest {

    @Test
    fun `recording adds every new amount to the pre-write paid total`() {
        val paidTotal = PaymentStatusResolver.paidTotalAfterRecording(BigDecimal("40"), listOf(BigDecimal("25"), BigDecimal("10")))

        assertEquals(0, BigDecimal("75").compareTo(paidTotal))
    }

    @Test
    fun `voiding subtracts the voided amount from the pre-void paid total`() {
        val paidTotal = PaymentStatusResolver.paidTotalAfterVoid(BigDecimal("100"), BigDecimal("30"))

        assertEquals(0, BigDecimal("70").compareTo(paidTotal))
    }
}
