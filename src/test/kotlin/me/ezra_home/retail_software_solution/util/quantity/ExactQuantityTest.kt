package me.ezra_home.retail_software_solution.util.quantity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode

class ExactQuantityTest {

    @Test
    fun `a whole quantity converts exactly to storage units`() {
        assertEquals(BigDecimal("18.0000"), ExactQuantity.ofUserProvided(BigDecimal("1"), 18L).toStorageQuantity())
        assertEquals(BigDecimal("26.0000"), ExactQuantity.ofUserProvided(BigDecimal("26"), 1L).toStorageQuantity())
    }

    @Test
    fun `a fractional root quantity and a fractional non-root quantity convert exactly`() {
        assertEquals(BigDecimal("0.5000"), ExactQuantity.ofUserProvided(BigDecimal("0.5"), 1L).toStorageQuantity())
        assertEquals(BigDecimal("4.5000"), ExactQuantity.ofUserProvided(BigDecimal("0.25"), 18L).toStorageQuantity())
    }

    @Test
    fun `selling 18000 single pieces consumes exactly 1000 bales`() {
        val onePiece = ExactQuantity.ofUserProvided(BigDecimal.ONE, 1L)
        val stock = ExactQuantity.ofUserProvided(BigDecimal("1000"), 18L)

        val remaining = (1..18_000).fold(stock) { balance, _ -> balance - onePiece }

        assertTrue(remaining.isZero())
        assertEquals(BigDecimal("0.0000"), remaining.toStorageQuantity())
    }

    @Test
    fun `sufficiency compares exactly with no tolerance`() {
        val available = ExactQuantity.ofStorage(BigDecimal("17.0000"))

        assertTrue(available >= ExactQuantity.ofStorage(BigDecimal("17")))
        assertTrue(available < ExactQuantity.ofStorage(BigDecimal("17.0001")))
    }

    @Test
    fun `equal quantities of different scale are equal`() {
        assertEquals(ExactQuantity.ofStorage(BigDecimal("2.50")), ExactQuantity.ofStorage(BigDecimal("2.5000")))
        assertEquals(ExactQuantity.ofStorage(BigDecimal("2.50")).hashCode(), ExactQuantity.ofStorage(BigDecimal("2.5000")).hashCode())
    }

    @Test
    fun `reading storage digits that would be dropped throws instead of rounding`() {
        assertThrows(ArithmeticException::class.java) { ExactQuantity.ofStorage(BigDecimal("1.00001")).toStorageQuantity() }
    }

    @Test
    fun `display in a larger unit divides once at scale 4 with the chosen mode`() {
        val eighteenPieces = ExactQuantity.ofUserProvided(BigDecimal("18"), 1L)
        val onePiece = ExactQuantity.ofUserProvided(BigDecimal.ONE, 1L)

        assertEquals(BigDecimal("1.0000"), eighteenPieces.inUnitOfSize(18L, RoundingMode.DOWN))
        assertEquals(BigDecimal("0.0555"), onePiece.inUnitOfSize(18L, RoundingMode.DOWN))
        assertEquals(BigDecimal("0.0556"), onePiece.inUnitOfSize(18L, RoundingMode.HALF_UP))
    }

    @Test
    fun `a storage unit per unit below 1 is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { ExactQuantity.ofUserProvided(BigDecimal.ONE, 0L) }
    }
}
