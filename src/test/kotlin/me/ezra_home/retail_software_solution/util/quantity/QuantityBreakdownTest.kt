package me.ezra_home.retail_software_solution.util.quantity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class QuantityBreakdownTest {

  private val pieceId = UUID.randomUUID()
  private val baleId = UUID.randomUUID()
  private val createdOn = OffsetDateTime.parse("2030-01-01T00:00:00Z")

  private val baleBreakdown = QuantityBreakdown.forBaseUnit(
    listOf(
      BreakdownUnit(pieceId, "Piece", 1, pieceId, createdOn),
      BreakdownUnit(baleId, "Bale", 18, pieceId, createdOn)
    ),
    baleId
  )

  @Test
  fun `a user-provided count carries thousands separators and drops trailing zeros`() {
    assertEquals("4,800 Bales", baleBreakdown.renderUserProvided(BigDecimal("4800.0000"), baleId))
    assertEquals("1,234,567.5 Bales", baleBreakdown.renderUserProvided(BigDecimal("1234567.5"), baleId))
    assertEquals("2.5 Bales", baleBreakdown.renderUserProvided(BigDecimal("2.5"), baleId))
  }

  @Test
  fun `a count of exactly one is singular and anything else is plural`() {
    assertEquals("1 Bale", baleBreakdown.renderUserProvided(BigDecimal.ONE, baleId))
    assertEquals("0 Bales", baleBreakdown.renderUserProvided(BigDecimal.ZERO, baleId))
  }

  @Test
  fun `a storage quantity breaks down with separators on every level`() {
    assertEquals("999 Bales 13 Pieces", baleBreakdown.render(ExactQuantity.ofStorage(BigDecimal(17995))))
    assertEquals("1,000 Bales 0.5 Pieces", baleBreakdown.render(ExactQuantity.ofStorage(BigDecimal("18000.5"))))
    assertEquals("-1,000 Bales", baleBreakdown.render(ExactQuantity.ofStorage(BigDecimal(-18000))))
  }

  @Test
  fun `a remainder rounds half up at four decimals`() {
    assertEquals("0.1235 Pieces", baleBreakdown.renderUserProvided(BigDecimal("0.12345"), pieceId))
  }
}
