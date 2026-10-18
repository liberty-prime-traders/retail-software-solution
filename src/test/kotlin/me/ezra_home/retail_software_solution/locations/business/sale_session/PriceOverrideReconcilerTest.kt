package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLineUpdateDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SessionIdentity
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentReasonService
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.SystemAdjustmentReason
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.PriceAdjustmentMethod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

class PriceOverrideReconcilerTest {

    private val priceOverrideReasonId = UUID.randomUUID()
    private val adjustmentReasonService = mock(AdjustmentReasonService::class.java).also {
        `when`(it.getSystemReasonId(SystemAdjustmentReason.PRICE_OVERRIDE)).thenReturn(priceOverrideReasonId)
    }
    private val reconciler = PriceOverrideReconciler(adjustmentReasonService)
    private val unitId = UUID.randomUUID()
    private val identity = SessionIdentity.mintFreshIdentity()

    private fun line(quantity: String, lineTotal: String, unit: UUID = unitId) = SaleSessionLine(
        identity = identity,
        locationProductId = UUID.randomUUID(),
        productLabel = "Widget",
        productPrintLabel = "Widget",
        userProvidedQuantity = BigDecimal(quantity),
        unitId = unit,
        baseUnitId = unitId,
        storageUnitsPerUnit = 1L,
        lineTotal = BigDecimal(lineTotal),
        defaultSalePrice = BigDecimal("1000.00"),
    )

    private fun update(quantity: String, override: String, unit: UUID = unitId) = SaleSessionLineUpdateDto(
        identity = identity, userProvidedQuantity = BigDecimal(quantity), unitId = unit, unitPriceOverride = BigDecimal(override)
    )

    private fun reconcile(pre: SaleSessionLine, post: SaleSessionLine, update: SaleSessionLineUpdateDto, existing: List<SaleSessionAdjustment> = emptyList()) =
        reconciler.reconcile(existing, mapOf(identity.key() to pre), mapOf(identity.key() to post), listOf(update))

    @Test
    fun `an override of 12 point 34 on a quantity of 2 point 50 stores a total difference and the derived unit price equals 12 point 34`() {
        val gross = line("2.50", "2500.0000")

        val adjustments = reconcile(gross, gross, update("2.50", "12.34"))

        val override = adjustments.single()
        assertEquals(PriceAdjustmentMethod.FIXED_LINE_TOTAL, override.calculationMethod)
        assertEquals(AdjustmentDirection.DISCOUNT, override.direction)
        assertEquals(BigDecimal("2469.1500"), override.value)
        val overrideTotal = gross.lineTotal - override.value
        assertEquals(BigDecimal("12.3400"), overrideTotal.divide(BigDecimal("2.50"), 4, RoundingMode.HALF_UP))
    }

    @Test
    fun `an override above the computed total is a surcharge of the total difference`() {
        val gross = line("2", "2000.0000")

        val override = reconcile(gross, gross, update("2", "1500.00")).single()

        assertEquals(AdjustmentDirection.SURCHARGE, override.direction)
        assertEquals(BigDecimal("1000.0000"), override.value)
    }

    @Test
    fun `an override equal to the computed total stores no adjustment`() {
        val gross = line("2", "2000.0000")

        assertEquals(emptyList<SaleSessionAdjustment>(), reconcile(gross, gross, update("2", "1000.00")))
    }

    @Test
    fun `echoing a derived unit price back does not create a phantom override`() {
        val piecesLine = line("18", "1000.0000")

        val adjustments = reconcile(piecesLine, piecesLine, update("18", "55.5556"))

        assertEquals(emptyList<SaleSessionAdjustment>(), adjustments)
    }

    @Test
    fun `a new typed price with three decimals is a business error`() {
        val gross = line("2", "2000.0000")

        assertThrows(RtsGenericException::class.java) {
            reconcile(gross, gross, update("2", "12.345"))
        }
    }

    @Test
    fun `a quantity change recomputes an existing override against the updated line`() {
        val pre = line("2.50", "2500.0000")
        val existingOverride = reconcile(pre, pre, update("2.50", "12.34")).single()
        val post = line("3", "3000.0000")

        val adjustments = reconcile(pre, post, update("3", "12.34"), listOf(existingOverride))

        assertEquals(BigDecimal("2962.9800"), adjustments.single().value)
    }

    @Test
    fun `changing the unit without a new price clears the override`() {
        val pre = line("2.50", "2500.0000")
        val existingOverride = reconcile(pre, pre, update("2.50", "12.34")).single()
        val otherUnit = UUID.randomUUID()
        val post = line("2.50", "2500.0000", unit = otherUnit)

        val adjustments = reconcile(pre, post, update("2.50", "12.34", unit = otherUnit), listOf(existingOverride))

        assertNull(adjustments.firstOrNull { it.isPriceOverride(priceOverrideReasonId) })
    }
}
