package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SessionIdentity
import org.mapstruct.Context
import org.mapstruct.Qualifier
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class SessionIdentityKey

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class SessionIdentityExistingId

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class SaleSessionLineTotal

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class SaleSessionLineCount

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class AdjustmentReasonLabel

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class AdjustmentCalculatedAmount

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class PaymentMethodName

@Component
object SaleSessionQualifier {

    @SessionIdentityKey
    fun toSessionIdentityKey(sessionIdentity: SessionIdentity?): UUID? = sessionIdentity?.key()

    @SessionIdentityExistingId
    fun toSessionIdentityExistingId(sessionIdentity: SessionIdentity?): UUID? = sessionIdentity?.id

    @SaleSessionLineTotal
    fun toSaleSessionLineTotal(saleSessionLine: SaleSessionLine): BigDecimal = saleSessionLine.lineTotal()

    @SaleSessionLineCount
    fun toSaleSessionLineCount(saleSession: SaleSession): Int = saleSession.saleLines.size

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
}
