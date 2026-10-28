package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.PriceAdjustmentMethod
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class SaleSessionAdjustmentTest {

    @Test
    fun `the related line key is the key of the line the adjustment is tied to`() {
        val lineIdentity = SessionIdentity.mintFreshIdentity()

        assertEquals(lineIdentity.key(), adjustment(lineIdentity).requiredRelatedSaleLineKey())
    }

    @Test
    fun `an order-level adjustment has no related line key`() {
        assertThrows(IllegalStateException::class.java) { adjustment(null).requiredRelatedSaleLineKey() }
    }

    private fun adjustment(relatedSaleLineIdentity: SessionIdentity?) = SaleSessionAdjustment(
        identity = SessionIdentity.mintFreshIdentity(),
        relatedSaleLineIdentity = relatedSaleLineIdentity,
        adjustmentReasonId = UUID.randomUUID(),
        direction = AdjustmentDirection.DISCOUNT,
        calculationMethod = PriceAdjustmentMethod.PERCENTAGE,
        value = BigDecimal.TEN,
        note = null,
        approvedById = null
    )
}
