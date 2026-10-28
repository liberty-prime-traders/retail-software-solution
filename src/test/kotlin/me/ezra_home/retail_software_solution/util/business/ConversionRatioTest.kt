package me.ezra_home.retail_software_solution.util.business

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class ConversionRatioTest {

    @Test
    fun `rejects a zero denominator`() {
        assertThrows<IllegalArgumentException> { ConversionRatio(1L, 0L) }
    }

    @Test
    fun `rejects a negative denominator`() {
        assertThrows<IllegalArgumentException> { ConversionRatio(1L, -2L) }
    }

    @Test
    fun `rejects a zero numerator`() {
        assertThrows<IllegalArgumentException> { ConversionRatio(0L, 1L) }
    }

    @Test
    fun `rejects a negative numerator`() {
        assertThrows<IllegalArgumentException> { ConversionRatio(-1L, 2L) }
    }

    @Test
    fun `reduced divides out the gcd`() {
        val ratio = ConversionRatio(24L, 6L).reduced()
        assertEquals(ConversionRatio(4L, 1L), ratio)
    }

    @Test
    fun `buildFromParts hands the parts to the builder`() {
        assertEquals(Pair(3L, 7L), ConversionRatio(3L, 7L).buildFromParts { numerator, denominator -> Pair(numerator, denominator) })
    }
}
