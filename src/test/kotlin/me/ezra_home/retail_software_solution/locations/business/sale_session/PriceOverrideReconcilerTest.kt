package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.support.WithDefaultOrganizationSession
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.PriceAdjustmentMethod
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLineUpdateDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SessionIdentity
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentReasonService
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.SystemAdjustmentReason
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.PositiveUserProvidedQuantity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

@WithDefaultOrganizationSession
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
        storageUnitsPerUserProvidedUnit = 1L,
        lineTotal = BigDecimal(lineTotal),
        defaultSalePrice = BigDecimal("1000.00"),
    )

    private fun update(quantity: String, override: String?, unit: UUID = unitId) = SaleSessionLineUpdateDto(
        identity = identity, userProvidedQuantity = PositiveUserProvidedQuantity.of(BigDecimal(quantity)), unitId = unit, unitPriceOverride = override?.let { Money.of(BigDecimal(it)) }
    )

    private fun reconcile(pre: SaleSessionLine, post: SaleSessionLine, update: SaleSessionLineUpdateDto, existing: List<SaleSessionAdjustment> = emptyList()) =
        reconciler.reconcile(existing, mapOf(identity.key() to pre), mapOf(identity.key() to post), listOf(update))

    @Test
    fun `an override of 12 point 34 on a quantity of 2 point 50 stores the typed price`() {
        val gross = line("2.50", "2500.0000")

        val adjustments = reconcile(gross, gross, update("2.50", "12.34"))

        val override = adjustments.single()
        assertEquals(PriceAdjustmentMethod.UNIT_PRICE_OVERRIDE, override.calculationMethod)
        assertEquals(AdjustmentDirection.DISCOUNT, override.direction)
        assertEquals(0, BigDecimal("12.34").compareTo(override.value))
    }

    @Test
    fun `an override above the computed total is a surcharge of the total difference`() {
        val gross = line("2", "2000.0000")

        val override = reconcile(gross, gross, update("2", "1500.00")).single()

        assertEquals(AdjustmentDirection.SURCHARGE, override.direction)
        assertEquals(0, BigDecimal("1500.00").compareTo(override.value))
    }

    @Test
    fun `an override equal to the computed total stores no adjustment`() {
        val gross = line("2", "2000.0000")

        assertEquals(emptyList<SaleSessionAdjustment>(), reconcile(gross, gross, update("2", "1000.00")))
    }

    @Test
    fun `an update with no typed price does not create an override`() {
        val piecesLine = line("18", "1000.0000")

        val adjustments = reconcile(piecesLine, piecesLine, update("18", null))

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
    fun `a quantity change keeps the existing override because it stores the typed price`() {
        val pre = line("2.50", "2500.0000")
        val existingOverride = reconcile(pre, pre, update("2.50", "12.34")).single()
        val post = line("3", "3000.0000")

        assertEquals(listOf(existingOverride), reconcile(pre, post, update("3", "12.34"), listOf(existingOverride)))
        assertEquals(listOf(existingOverride), reconcile(pre, post, update("3", null), listOf(existingOverride)))
    }

    @Test
    fun `a sub-cent override keeps its typed price through a quantity edit`() {
        val pre = line("0.25", "250.0000")
        val existingOverride = reconcile(pre, pre, update("0.25", "0.15")).single()
        val post = line("4", "4000.0000")

        val adjustment = reconcile(pre, post, update("4", null), listOf(existingOverride)).single()

        assertEquals(existingOverride.identity, adjustment.identity)
        assertEquals(0, BigDecimal("0.15").compareTo(adjustment.value))
    }

    @Test
    fun `typing a different price replaces the override under a new identity`() {
        val gross = line("2", "2000.0000")
        val existingOverride = reconcile(gross, gross, update("2", "500.00")).single()

        val replacement = reconcile(gross, gross, update("2", "400.00"), listOf(existingOverride)).single()

        assertNotEquals(existingOverride.identity, replacement.identity)
        assertEquals(0, BigDecimal("400.00").compareTo(replacement.value))
    }

    @Test
    fun `typing the list price removes an existing override`() {
        val gross = line("2", "2000.0000")
        val existingOverride = reconcile(gross, gross, update("2", "500.00")).single()

        assertEquals(emptyList<SaleSessionAdjustment>(), reconcile(gross, gross, update("2", "1000.00"), listOf(existingOverride)))
    }

    @Test
    fun `changing the unit without a new price clears the override`() {
        val pre = line("2.50", "2500.0000")
        val existingOverride = reconcile(pre, pre, update("2.50", "12.34")).single()
        val otherUnit = UUID.randomUUID()
        val post = line("2.50", "2500.0000", unit = otherUnit)

        val adjustments = reconcile(pre, post, update("2.50", null, unit = otherUnit), listOf(existingOverride))

        assertNull(adjustments.firstOrNull { it.isPriceOverride(priceOverrideReasonId) })
    }

    @Test
    fun `typing the money-scale rounding of a fractional list price stores no override`() {
        val piecesLine = line("9", "500.0000")

        assertEquals(emptyList<SaleSessionAdjustment>(), reconcile(piecesLine, piecesLine, update("9", "55.56")))
    }

    @Test
    fun `typing the money-scale rounding of a fractional list price removes an existing override`() {
        val piecesLine = line("9", "500.0000")
        val existingOverride = reconcile(piecesLine, piecesLine, update("9", "55.57")).single()

        assertEquals(emptyList<SaleSessionAdjustment>(), reconcile(piecesLine, piecesLine, update("9", "55.56"), listOf(existingOverride)))
    }

    @Test
    fun `a price one cent away from the rounded fractional list price is still an override`() {
        val piecesLine = line("9", "500.0000")

        val override = reconcile(piecesLine, piecesLine, update("9", "55.57")).single()

        assertEquals(AdjustmentDirection.SURCHARGE, override.direction)
        assertEquals(0, BigDecimal("55.57").compareTo(override.value))
    }
}
