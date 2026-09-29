package me.ezra_home.retail_software_solution.util.business

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
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
    fun `factor divides numerator by denominator at scale 4`() {
        val ratio = ConversionRatio(1L, 3L)
        assertEquals(BigDecimal("0.3333"), ratio.factor())
    }

    @Test
    fun `applyTo multiplies then divides at scale 4`() {
        val ratio = ConversionRatio(1L, 2L)
        assertEquals(BigDecimal("5.0000"), ratio.applyTo(BigDecimal("10")))
    }

    @Test
    fun `invert swaps numerator and denominator`() {
        val ratio = ConversionRatio(1L, 2L)
        assertEquals(ConversionRatio(2L, 1L), ratio.invert())
    }

    @Test
    fun `times compounds two ratios along a conversion chain`() {
        // 1 case = 4 boxes, 1 box = 6 units -> 1 case = 24 units
        val caseToBox = ConversionRatio(4L, 1L)
        val boxToUnit = ConversionRatio(6L, 1L)
        assertEquals(ConversionRatio(24L, 1L), caseToBox.times(boxToUnit))
    }

    @Test
    fun `times reduces a compound chain that lands on a common factor`() {
        // 1 halfLitre = 1/2 litre, 1 litre = 2/3 something -> compounds to 1/3, not 2/6
        val halfLitreToLitre = ConversionRatio(1L, 2L)
        val litreToSomething = ConversionRatio(2L, 3L)
        assertEquals(ConversionRatio(1L, 3L), halfLitreToLitre.times(litreToSomething))
    }

    @Test
    fun `identity applyTo returns the same quantity`() {
        assertEquals(BigDecimal("7.0000"), ConversionRatio.IDENTITY.applyTo(BigDecimal("7")))
    }

    @Test
    fun `isEquivalentTo is true for unreduced forms of the same ratio`() {
        assertEquals(true, ConversionRatio(2L, 4L).isEquivalentTo(ConversionRatio(1L, 2L)))
    }

    @Test
    fun `isEquivalentTo is false for genuinely different ratios`() {
        assertEquals(false, ConversionRatio(1L, 2L).isEquivalentTo(ConversionRatio(1L, 3L)))
    }
}
