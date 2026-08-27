package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.purchase.api.PaymentStatus
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineDto
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.PaymentStatusResolver
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionUiOptions
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SessionIdentity
import me.ezra_home.retail_software_solution.util.business.Decimals
import org.mapstruct.Context
import org.springframework.stereotype.Component
import java.math.BigDecimal
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
        PaymentStatusResolver.resolve(saleSession.totals.paymentTotal, saleSession.totals.payableTotal)

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

    @SaleLineDefaultSalePrice
    fun deriveDefaultSalePrice(saleLineDto: SaleLineDto): BigDecimal =
        Decimals.divideScale4(saleLineDto.unitPrice, saleLineDto.conversionRatio.factor())

    @LineConversionFactor
    fun toConversionFactor(saleSessionLine: SaleSessionLine): BigDecimal = saleSessionLine.conversionRatio.factor()

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
