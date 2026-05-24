package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.CalculationMethod
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

data class SaleSessionStartDto(
    val contactId: UUID? = null,
    val saleId: UUID? = null,
) {
    init {
        if (contactId == null && saleId == null) {
            throw RtsGenericException("Either contactId or saleId must be provided when starting a new Sale Session")
        }
    }
}

data class SaleSessionLineAddDto(
    val locationProductId: UUID,
    val quantity: BigDecimal,
)

data class SaleSessionLineUpdateDto(
    val identity: SessionIdentity,
    val quantity: BigDecimal,
    val unitId: UUID,
)

data class SaleSessionLineRequestDto(
    val additions: List<SaleSessionLineAddDto> = emptyList(),
    val updates: List<SaleSessionLineUpdateDto> = emptyList(),
)

data class SaleSessionRowIdentityDto(
    val identity: SessionIdentity,
)

data class SaleSessionAdjustmentAddDto(
    val relatedSaleLineIdentity: SessionIdentity? = null,
    val adjustmentReasonId: UUID,
    val direction: AdjustmentDirection,
    val calculationMethod: CalculationMethod,
    val value: BigDecimal,
    val note: String? = null,
    val approvedById: UUID? = null,
)

data class SaleSessionPaymentAddDto(
    val paymentMethodId: UUID,
    val amount: BigDecimal,
    val reference: String? = null,
    val paymentDate: OffsetDateTime? = null,
)

data class SaleSessionPaymentRemoveDto(
    val identity: SessionIdentity,
    val voidReason: String? = null,
)

data class SaleSessionHeaderUpdateDto(
    val contactId: Optional<UUID>? = null,
    val soldById: Optional<UUID>? = null,
    val dateSold: Optional<OffsetDateTime>? = null,
    val notes: Optional<String>? = null,
)
