package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustmentResponse
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLineResponse
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionPayment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionPaymentResponse
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionResponseDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionSummaryDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import java.util.UUID

@Mapper(config = RtsMapperConfig::class, uses = [SaleSessionQualifierUtil::class])
interface SessionToResponseMapper {

    @Mapping(source = "header.referenceNumber", target = "referenceNumber")
    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    @Mapping(source = "lastAccessedById", target = "lastAccessedBy", qualifiedBy = [FullName::class])
    @Mapping(source = "header.contactId", target = "contactId")
    @Mapping(target = "contactLabel", expression = "java(sessionMappingContext.getContactLabel())")
    @Mapping(target = "walkInCustomer", expression = "java(sessionMappingContext.getWalkInCustomer())")
    @Mapping(target = "showActiveUserWarning", expression = "java(sessionMappingContext.getShowActiveUserWarning())")
    @Mapping(source = "sessionId", target = "id")
    @Mapping(source = "header.soldById", target = "soldBy", qualifiedBy = [FullName::class])
    @Mapping(source = "header.dateSold", target = "dateSold")
    @Mapping(source = "header.notes", target = "notes")
    @Mapping(source = "originalStatus", target = "saleStatus")
    @Mapping(source = ".", target = "paymentStatus", qualifiedBy = [SaleSessionPaymentStatus::class])
    @Mapping(source = ".", target = "uiOptions", qualifiedBy = [SaleSessionUiOptionsBuild::class])
    fun toResponseDto(
        saleSession: SaleSession,
        @Context sessionMappingContext: SaleSessionMappingContext,
        @Context adjustmentMappingContext: AdjustmentMappingContext,
        @Context lineMappingContext: LineMappingContext,
        @Context paymentMethodNamesById: Map<UUID, String>,
    ): SaleSessionResponseDto

    @Mapping(target = "unitPriceOverride", source = ".", qualifiedBy = [LineUnitPriceOverride::class])
    @Mapping(target = "netUnitPrice", source = ".", qualifiedBy = [LineNetUnitPrice::class])
    fun toLineDto(
        saleSessionLine: SaleSessionLine,
        @Context lineMappingContext: LineMappingContext,
    ): SaleSessionLineResponse

    @Mapping(source = "adjustmentReasonId", target = "adjustmentReason", qualifiedBy = [AdjustmentReasonLabel::class])
    @Mapping(source = ".", target = "calculatedAmount", qualifiedBy = [AdjustmentCalculatedAmount::class])
    @Mapping(source = "approvedById", target = "approvedBy", qualifiedBy = [FullName::class])
    fun toAdjustmentDto(
        saleSessionAdjustment: SaleSessionAdjustment,
        @Context adjustmentMappingContext: AdjustmentMappingContext,
    ): SaleSessionAdjustmentResponse

    @Mapping(source = "paymentMethodId", target = "paymentMethod", qualifiedBy = [PaymentMethodName::class])
    fun toPaymentDto(
        saleSessionPayment: SaleSessionPayment,
        @Context paymentMethodNamesById: Map<UUID, String>,
    ): SaleSessionPaymentResponse

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    @Mapping(source = "lastAccessedById", target = "lastAccessedBy", qualifiedBy = [FullName::class])
    @Mapping(target = "contactLabel", expression = "java(contactLabel)")
    @Mapping(source = "totals.payableTotal", target = "payableTotal")
    @Mapping(source = "sessionId", target = "id")
    fun toSummaryDto(
        saleSession: SaleSession,
        @Context contactLabel: String,
    ): SaleSessionSummaryDto
}
