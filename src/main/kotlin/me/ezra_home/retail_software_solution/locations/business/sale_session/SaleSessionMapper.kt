package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale_session.api.ActiveSessionUser
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustmentDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLineDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionPayment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionPaymentDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionResponseDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionSummaryDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import java.math.BigDecimal
import java.util.UUID

@Mapper(config = RtsMapperConfig::class)
interface SaleSessionMapper {

    @Mapping(source = "createdById", target = "createdByLabel", qualifiedBy = [FullName::class])
    @Mapping(source = "lastAccessedById", target = "lastAccessedByLabel", qualifiedBy = [FullName::class])
    @Mapping(source = "header.contactId", target = "contactId")
    @Mapping(target = "contactLabel", expression = "java(ctx.getContactLabel())")
    @Mapping(target = "walkInCustomer", expression = "java(ctx.getWalkInCustomer())")
    @Mapping(source = "header.soldById", target = "soldById")
    @Mapping(source = "header.soldById", target = "soldByLabel", qualifiedBy = [FullName::class])
    @Mapping(source = "header.dateSold", target = "dateSold")
    @Mapping(source = "header.notes", target = "notes")
    fun toResponseDto(
        session: SaleSession,
        @Context ctx: SaleSessionMappingContext,
        @Context adjustmentCtx: AdjustmentMappingContext,
        @Context paymentMethodNames: Map<UUID, String>,
    ): SaleSessionResponseDto

    @Mapping(target = "lineTotal", expression = "java(line.lineTotal())")
    fun toLineDto(line: SaleSessionLine): SaleSessionLineDto

    fun toLineDtos(lines: List<SaleSessionLine>): List<SaleSessionLineDto>

    @Mapping(target = "adjustmentReasonLabel", expression = "java(ctx.reasonLabel(adjustment.getAdjustmentReasonId()))")
    @Mapping(target = "calculatedAmount", expression = "java(ctx.calculatedAmount(adjustment))")
    @Mapping(source = "approvedById", target = "approvedByLabel", qualifiedBy = [FullName::class])
    fun toAdjustmentDto(
        adjustment: SaleSessionAdjustment,
        @Context ctx: AdjustmentMappingContext,
    ): SaleSessionAdjustmentDto

    fun toAdjustmentDtos(
        adjustments: List<SaleSessionAdjustment>,
        @Context ctx: AdjustmentMappingContext,
    ): List<SaleSessionAdjustmentDto>

    @Mapping(target = "paymentMethod", expression = "java(paymentMethodNames.get(payment.getPaymentMethodId()))")
    fun toPaymentDto(
        payment: SaleSessionPayment,
        @Context paymentMethodNames: Map<UUID, String>,
    ): SaleSessionPaymentDto

    fun toPaymentDtos(
        payments: List<SaleSessionPayment>,
        @Context paymentMethodNames: Map<UUID, String>,
    ): List<SaleSessionPaymentDto>

    @Mapping(source = "createdById", target = "createdByLabel", qualifiedBy = [FullName::class])
    @Mapping(source = "header.contactId", target = "contactId")
    @Mapping(target = "contactLabel", expression = "java(ctx.getContactLabel())")
    @Mapping(target = "lineCount", expression = "java(session.getLines().size())")
    @Mapping(source = "totals.payableTotal", target = "payableTotal")
    @Mapping(target = "activeUser", expression = "java(ctx.getActiveUser())")
    fun toSummaryDto(
        session: SaleSession,
        @Context ctx: SaleSessionSummaryContext,
    ): SaleSessionSummaryDto
}

data class SaleSessionSummaryContext(
    val contactLabel: String,
    val activeUser: ActiveSessionUser?,
)

data class SaleSessionMappingContext(
    val contactLabel: String,
    val walkInCustomer: Boolean,
)

data class AdjustmentMappingContext(
    private val reasonNamesById: Map<UUID, String>,
    private val totalsCalculator: SaleSessionTotalsCalculator,
    private val lines: List<SaleSessionLine>,
) {
    fun reasonLabel(reasonId: UUID): String? = reasonNamesById[reasonId]
    fun calculatedAmount(adjustment: SaleSessionAdjustment): BigDecimal =
        totalsCalculator.calculatedAmount(adjustment, lines)
}
