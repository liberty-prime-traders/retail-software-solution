package me.ezra_home.retail_software_solution.util.quantity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class QuantityBreakdownTest {

    private val epoch = OffsetDateTime.parse("2026-01-01T00:00:00Z")
    private val piece = unit("Piece", 1L, rootUnitId = null)
    private val bale = unit("Bale", 18L, piece.id, createdOffsetMinutes = 1)

    private fun unit(name: String, sizeInRoot: Long, rootUnitId: UUID?, createdOffsetMinutes: Long = 0, id: UUID = UUID.randomUUID()) =
        BreakdownUnit(id, name, sizeInRoot, rootUnitId ?: id, epoch.plusMinutes(createdOffsetMinutes))

    private fun render(units: List<BreakdownUnit>, baseUnit: BreakdownUnit, storageQuantity: String) =
        QuantityBreakdown.forBaseUnit(units, baseUnit.id).render(ExactQuantity.ofStorage(BigDecimal(storageQuantity)))

    @Test
    fun `26 pieces is 1 Bale 8 Pieces and 17995 pieces is 999 Bales 13 Pieces`() {
        assertEquals("1 Bale 8 Pieces", render(listOf(piece, bale), bale, "26"))
        assertEquals("999 Bales 13 Pieces", render(listOf(piece, bale), bale, "17995"))
    }

    @Test
    fun `the root level carries a fractional remainder with trailing zeros stripped`() {
        assertEquals("2 Bales 0.5 Pieces", render(listOf(piece, bale), bale, "36.5"))
        assertEquals("0.25 Pieces", render(listOf(piece, bale), bale, "0.2500"))
    }

    @Test
    fun `zero levels are skipped and zero stock shows 0 in the base unit`() {
        assertEquals("1 Bale", render(listOf(piece, bale), bale, "18"))
        assertEquals("8 Pieces", render(listOf(piece, bale), bale, "8"))
        assertEquals("0 Bales", render(listOf(piece, bale), bale, "0"))
    }

    @Test
    fun `a product whose base unit is the root shows only the root level`() {
        assertEquals("26 Pieces", render(listOf(piece, bale), piece, "26"))
        assertEquals("0 Pieces", render(listOf(piece, bale), piece, "0"))
    }

    @Test
    fun `units larger than the base unit are never used`() {
        val dozen = unit("Dozen", 12L, piece.id, 2)
        val carton = unit("Carton", 48L, piece.id, 3)

        assertEquals("1 Dozen 5 Pieces", render(listOf(piece, dozen, carton), dozen, "17"))
        assertEquals("1 Carton 1 Dozen 5 Pieces", render(listOf(piece, dozen, carton), carton, "65"))
    }

    @Test
    fun `carton of 48 and box of 12 break 103 pieces into 2 Cartons 7 Pieces`() {
        val box = unit("Box", 12L, piece.id, 2)
        val carton = unit("Carton", 48L, piece.id, 3)

        assertEquals("2 Cartons 7 Pieces", render(listOf(piece, box, carton), carton, "103"))
    }

    @Test
    fun `sibling units take the larger one greedily`() {
        val box = unit("Box", 10L, piece.id, 2)
        val dozen = unit("Dozen", 12L, piece.id, 3)
        val wide = unit("Gross", 144L, piece.id, 4)

        assertEquals("8 Dozens 7 Pieces", render(listOf(piece, box, dozen), dozen, "103"))
        assertEquals("10 Boxs 3 Pieces", render(listOf(piece, box, dozen), box, "103"))
        assertEquals("1 Gross", render(listOf(piece, box, dozen, wide), wide, "144"))
    }

    @Test
    fun `equal-size units are taken in created-on then id order`() {
        val earlierDozen = unit("Dozen", 12L, piece.id, 2)
        val laterDozen = unit("Doz", 12L, piece.id, 5)

        assertEquals("1 Bale 1 Dozen", render(listOf(piece, laterDozen, earlierDozen, bale.copy(sizeInRoot = 24L)), bale.copy(sizeInRoot = 24L), "36"))
        assertEquals("1 Dozen 1 Piece", render(listOf(piece, laterDozen, earlierDozen), earlierDozen, "13"))
    }

    @Test
    fun `a non-base unit the size of the base unit is skipped`() {
        val sibling = unit("Bundle", 18L, piece.id, 5)

        assertEquals("1 Bale 8 Pieces", render(listOf(piece, bale, sibling), bale, "26"))
    }

    @Test
    fun `a miscellaneous product renders only its own unit`() {
        val miscPiece = unit("Piece", 1L, rootUnitId = null)
        val miscCarton = unit("Carton", 1L, rootUnitId = null)

        assertEquals("5 Pieces", render(listOf(miscPiece, miscCarton, piece, bale), miscPiece, "5"))
    }

    @Test
    fun `renaming a unit changes a live render but a stored text is a plain string`() {
        val before = render(listOf(piece, bale), bale, "26")
        val renamedBale = bale.copy(name = "Sack")

        assertEquals("1 Bale 8 Pieces", before)
        assertEquals("1 Sack 8 Pieces", render(listOf(piece, renamedBale), renamedBale, "26"))
    }

    @Test
    fun `a typed quantity renders as typed in its own unit, pluralized, never broken down`() {
        val breakdown = QuantityBreakdown.forBaseUnit(listOf(piece, bale), bale.id)

        assertEquals("1 Bale", breakdown.renderUserProvided(BigDecimal("1.00"), bale.id))
        assertEquals("2.5 Bales", breakdown.renderUserProvided(BigDecimal("2.50"), bale.id))
        assertEquals("26 Pieces", breakdown.renderUserProvided(BigDecimal("26"), piece.id))
    }
}
