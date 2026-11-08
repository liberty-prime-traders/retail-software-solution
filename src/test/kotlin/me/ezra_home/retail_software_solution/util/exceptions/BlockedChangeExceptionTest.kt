package me.ezra_home.retail_software_solution.util.exceptions

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BlockedChangeExceptionTest {

    private fun problem(documentReference: String) = BlockingProblem(documentReference, "what", "do this")

    @Test
    fun `no problems means nothing is thrown`() {
        assertDoesNotThrow { BlockedChangeException.throwIfAny(emptyList()) }
    }

    @Test
    fun `the headline counts the problems and the payload lists them`() {
        val one = assertThrows(BlockedChangeException::class.java) {
            BlockedChangeException.throwIfAny(listOf(problem("SAL1")))
        }
        val two = assertThrows(BlockedChangeException::class.java) {
            BlockedChangeException.throwIfAny(listOf(problem("SAL1"), problem("SAL2")))
        }

        assertEquals("This change cannot be saved. 1 thing needs fixing.", one.message)
        assertEquals("This change cannot be saved. 2 things need fixing.", two.message)
        assertEquals(listOf(problem("SAL1"), problem("SAL2")), two.payload)
    }

    @Test
    fun `a support problem keeps a headline that says so`() {
        val exception = BlockedChangeException.contactSupport(listOf(problem("SAL1")))

        assertEquals(true, exception.message.contains("contact support"))
    }
}
