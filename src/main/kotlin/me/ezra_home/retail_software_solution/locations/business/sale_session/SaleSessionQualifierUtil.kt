package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import me.ezra_home.retail_software_solution.util.business.PaymentStatusResolver
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionUiOptions
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SessionIdentity
import me.ezra_home.retail_software_solution.util.quantity.BaseUnitDisplay
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.UserProvidedQuantity
import org.mapstruct.Context
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID


@Component
object SaleSessionQualifierUtil {

    @SessionIdentityKey
    fun toSessionIdentityKey(sessionIdentity: SessionIdentity?): UUID? = sessionIdentity?.key()

    @SessionIdentityExistingId
    fun toSessionIdentityExistingId(sessionIdentity: SessionIdentity?): UUID? = sessionIdentity?.id

    @PersistedSessionIdentity
    fun toPersistedSessionIdentity(id: UUID): SessionIdentity = SessionIdentity.persisted(id)

    @RelatedSaleLineSessionIdentity
    fun toRelatedSaleLineSessionIdentity(
        saleLineId: UUID?,
        @Context relatedSaleLineIdentityBySaleLineId: Map<UUID, SessionIdentity>,
    ): SessionIdentity? = saleLineId?.let { relatedSaleLineIdentityBySaleLineId[it] }

    @SaleSessionUiOptionsBuild
    fun toSaleSessionUiOptions(
        saleSession: SaleSession,
        @Context sessionMappingContext: SaleSessionMappingContext
    ): SaleSessionUiOptions = SaleSessionUiOptions(
        canMakeChangesToTheSale = saleSession.mutable(),
        canAddPaymentsToSale = saleSession.canAddPayments(),
        showUnreservedChangesWarning = saleSession.showUnreservedChangesWarning,
        showActiveUserWarning = sessionMappingContext.showActiveUserWarning
    )

    @SaleSessionPaymentStatus
    fun toSaleSessionPaymentStatus(saleSession: SaleSession): PaymentStatus =
        PaymentStatusResolver.resolve(saleSession.totals.paymentTotal, saleSession.totals.receivableTotal)

    @AdjustmentReasonLabel
    fun toAdjustmentReasonLabel(
        adjustmentReasonId: UUID,
        @Context adjustmentMappingContext: AdjustmentMappingContext,
    ): String? = adjustmentMappingContext.reasonLabel(adjustmentReasonId)

    @AdjustmentCalculatedAmount
    fun toAdjustmentCalculatedAmount(
        saleSessionAdjustment: SaleSessionAdjustment,
        @Context adjustmentMappingContext: AdjustmentMappingContext,
    ): BigDecimal = adjustmentMappingContext.calculatedAmount(saleSessionAdjustment)

    @PaymentMethodName
    fun toPaymentMethodName(
        paymentMethodId: UUID,
        @Context paymentMethodNamesById: Map<UUID, String>,
    ): String? = paymentMethodNamesById[paymentMethodId]

    @LineConversionFactor
    fun toConversionFactor(
        saleSessionLine: SaleSessionLine,
        @Context lineMappingContext: LineMappingContext,
    ): BigDecimal = BaseUnitDisplay.conversionFactor(
        saleSessionLine.storageUnitsPerUserProvidedUnit, lineMappingContext.storageUnitsPerBaseUnitFor(saleSessionLine)
    )

    @LineBaseQuantity
    fun toLineBaseQuantity(
        saleSessionLine: SaleSessionLine,
        @Context lineMappingContext: LineMappingContext,
    ): BigDecimal = BaseUnitDisplay.baseQuantity(
        UserProvidedQuantity.of(saleSessionLine.userProvidedQuantity),
        saleSessionLine.storageUnitsPerUserProvidedUnit,
        lineMappingContext.storageUnitsPerBaseUnitFor(saleSessionLine),
        RoundingMode.DOWN,
    )

    // On hand and available round down and reserved rounds up, so the displayed available never exceeds what can be sold.
    @LineQuantityOnHandInBaseUnit
    fun toLineQuantityOnHandInBaseUnit(
        saleSessionLine: SaleSessionLine,
        @Context lineMappingContext: LineMappingContext,
    ): BigDecimal = BaseUnitDisplay.storageQuantityInBaseUnit(
        saleSessionLine.storageQuantityOnHand, lineMappingContext.storageUnitsPerBaseUnitFor(saleSessionLine), RoundingMode.DOWN
    )

    @LineQuantityReservedInBaseUnit
    fun toLineQuantityReservedInBaseUnit(
        saleSessionLine: SaleSessionLine,
        @Context lineMappingContext: LineMappingContext,
    ): BigDecimal = BaseUnitDisplay.storageQuantityInBaseUnit(
        saleSessionLine.storageQuantityReserved, lineMappingContext.storageUnitsPerBaseUnitFor(saleSessionLine), RoundingMode.UP
    )

    @LineQuantityAvailableInBaseUnit
    fun toLineQuantityAvailableInBaseUnit(
        saleSessionLine: SaleSessionLine,
        @Context lineMappingContext: LineMappingContext,
    ): BigDecimal = BaseUnitDisplay.storageQuantityInBaseUnit(
        saleSessionLine.storageQuantityAvailable, lineMappingContext.storageUnitsPerBaseUnitFor(saleSessionLine), RoundingMode.DOWN
    )

    @LineOnHandBreakdown
    fun toLineOnHandBreakdown(
        saleSessionLine: SaleSessionLine,
        @Context lineMappingContext: LineMappingContext,
    ): String = lineMappingContext.breakdownFor(saleSessionLine).render(ExactQuantity.ofStorage(saleSessionLine.storageQuantityOnHand))

    @LineAvailableBreakdown
    fun toLineAvailableBreakdown(
        saleSessionLine: SaleSessionLine,
        @Context lineMappingContext: LineMappingContext,
    ): String = lineMappingContext.breakdownFor(saleSessionLine).render(ExactQuantity.ofStorage(saleSessionLine.storageQuantityAvailable))

    @LineNetTotal
    fun toLineNetTotal(
        saleSessionLine: SaleSessionLine,
        @Context lineMappingContext: LineMappingContext,
    ): BigDecimal = lineMappingContext.netLineTotalFor(saleSessionLine)

    @SaleLineBaseUnitId
    fun resolveBaseUnitId(
        locationProductId: UUID,
        @Context baseUnitIdsByLocationProductId: Map<UUID, UUID>,
    ): UUID = baseUnitIdsByLocationProductId.getValue(locationProductId)

    @LineUnitPriceOverride
    fun toLineUnitPriceOverride(
        saleSessionLine: SaleSessionLine,
        @Context lineMappingContext: LineMappingContext,
    ): BigDecimal = lineMappingContext.unitPriceOverrideFor(saleSessionLine)

    @LineNetUnitPrice
    fun toLineNetUnitPrice(
        saleSessionLine: SaleSessionLine,
        @Context lineMappingContext: LineMappingContext,
    ): BigDecimal = lineMappingContext.netUnitPriceFor(saleSessionLine)
}
