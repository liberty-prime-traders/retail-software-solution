package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.PriceAdjustmentMethod
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionHeader
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionTotals
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SessionIdentity
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentReasonService
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.SystemAdjustmentReason
import me.ezra_home.retail_software_solution.support.WithDefaultOrganizationSession
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@WithDefaultOrganizationSession
class SaleSessionValidatorTest {

    private val adjustmentReasonService = mock(AdjustmentReasonService::class.java)
    private val saleSessionValidator = SaleSessionValidator(adjustmentReasonService)
    private val maximum = Decimals.MAXIMUM_TRANSACTION_AMOUNT

    private val priceOverrideReasonId = UUID.randomUUID()

    init {
        `when`(adjustmentReasonService.getSystemReasonId(SystemAdjustmentReason.PRICE_OVERRIDE)).thenReturn(priceOverrideReasonId)
    }

    @Test
    fun `the system price override reason is not accepted for an added adjustment, any other reason is`() {
        assertThrows(RtsGenericException::class.java) { saleSessionValidator.guardReasonIsNotPriceOverride(priceOverrideReasonId) }

        saleSessionValidator.guardReasonIsNotPriceOverride(UUID.randomUUID())
    }

    @Test
    fun `a session whose amounts sit exactly at the maximum is accepted`() {
        saleSessionValidator.validate(saleSession(listOf(saleSessionLine(maximum))))
    }

    @Test
    fun `a line total above the maximum is rejected before the database column can overflow`() {
        assertThrows(RtsGenericException::class.java) {
            saleSessionValidator.validate(saleSession(listOf(saleSessionLine(maximum + BigDecimal("0.01")))))
        }
    }

    @Test
    fun `lines that are each within the maximum are rejected when their subtotal is not`() {
        val saleSession = saleSession(listOf(saleSessionLine(BigDecimal("600000000000.00")), saleSessionLine(BigDecimal("600000000000.00"))))

        assertThrows(RtsGenericException::class.java) { saleSessionValidator.validate(saleSession) }
    }

    @Test
    fun `a price override whose price times quantity is huge is rejected even though a surcharge has no ceiling`() {
        val line = saleSessionLine(BigDecimal("10.00"), quantity = BigDecimal("1000"))
        val surcharge = priceOverrideAdjustment(line, maximum)

        assertThrows(RtsGenericException::class.java) {
            saleSessionValidator.validate(saleSession(listOf(line), listOf(surcharge)))
        }
    }

    private fun saleSessionLine(lineTotal: BigDecimal, quantity: BigDecimal = BigDecimal.ONE) = SaleSessionLine(
        identity = SessionIdentity.mintFreshIdentity(),
        locationProductId = UUID.randomUUID(),
        productLabel = "Rice",
        productPrintLabel = "Rice",
        userProvidedQuantity = quantity,
        unitId = UUID.randomUUID(),
        baseUnitId = UUID.randomUUID(),
        storageUnitsPerUserProvidedUnit = 1L,
        lineTotal = lineTotal,
        defaultSalePrice = lineTotal
    )

    private fun priceOverrideAdjustment(line: SaleSessionLine, unitPrice: BigDecimal) = SaleSessionAdjustment(
        identity = SessionIdentity.mintFreshIdentity(),
        relatedSaleLineIdentity = line.identity,
        adjustmentReasonId = priceOverrideReasonId,
        direction = AdjustmentDirection.SURCHARGE,
        calculationMethod = PriceAdjustmentMethod.UNIT_PRICE_OVERRIDE,
        value = unitPrice,
        note = null,
        approvedById = null
    )

    private fun saleSession(saleSessionLines: List<SaleSessionLine>, saleSessionAdjustments: List<SaleSessionAdjustment> = emptyList()) =
        SaleSession(
            sessionId = UUID.randomUUID(),
            locationId = UUID.randomUUID(),
            saleId = null,
            saleVersion = null,
            productsVersion = 0L,
            productsReservedAtVersion = 0L,
            originalStatus = SaleStatus.DRAFT,
            createdById = UUID.randomUUID(),
            createdAt = OffsetDateTime.now(),
            lastUpdatedAt = OffsetDateTime.now(),
            lastAccessedById = UUID.randomUUID(),
            lastAccessedAt = OffsetDateTime.now(),
            header = SaleSessionHeader(referenceNumber = null, contactId = UUID.randomUUID(), soldById = null, dateSold = null, notes = null),
            saleLines = saleSessionLines,
            saleAdjustments = saleSessionAdjustments,
            salePayments = emptyList(),
            totals = SaleSessionTotals.ZERO
        )
}
