package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductStockBreakdownProvider
import me.ezra_home.retail_software_solution.util.quantity.BreakdownUnit
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class PurchaseDeliveryValidatorTest {

    private val locationProductId = UUID.randomUUID()
    private val unitId = UUID.randomUUID()
    private val purchaseDeliveryValidator = PurchaseDeliveryValidator(
        mock(LocationProductDataFetcher::class.java).also {
            val summary = mock(LocationProductSummaryDto::class.java)
            `when`(summary.label).thenReturn("Sugar")
            `when`(it.findSummaryByIds(setOf(locationProductId))).thenReturn(mapOf(locationProductId to summary))
        },
        mock(ProductStockBreakdownProvider::class.java).also {
            `when`(it.forLocationProduct(locationProductId)).thenReturn(
                QuantityBreakdown.forBaseUnit(
                    listOf(BreakdownUnit(unitId, "pc", 1L, unitId, OffsetDateTime.parse("2026-11-01T10:00:00Z"))),
                    unitId
                )
            )
        }
    )

    @Test
    fun `a quantity within the maximum has no problem`() {
        assertNull(
            purchaseDeliveryValidator.maximumQuantityProblem(
                "DELN000001", locationProductId,
                ExactQuantity.ofStorage(BigDecimal("6")), ExactQuantity.ofStorage(BigDecimal("6"))
            )
        )
    }

    @Test
    fun `a quantity above the maximum says what is allowed and what to enter`() {
        val problem = purchaseDeliveryValidator.maximumQuantityProblem(
            "DELN000001", locationProductId,
            ExactQuantity.ofStorage(BigDecimal("6")), ExactQuantity.ofStorage(BigDecimal("7"))
        )!!

        assertEquals("DELN000001", problem.documentReference)
        assertEquals("You can deliver at most 6 pcs of 'Sugar', but you entered 7 pcs.", problem.description)
        assertEquals("Enter 6 pcs or less.", problem.effect)
    }
}
