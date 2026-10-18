package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import com.fasterxml.jackson.annotation.JsonProperty
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.PriceAdjustmentMethod
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.QuantityLimits
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
    @param:JsonProperty("quantity")
    val userProvidedQuantity: BigDecimal,
) {
    init {
        Decimals.requireMaxDecimalPlaces(userProvidedQuantity, QuantityLimits.MAX_USER_QUANTITY_DECIMAL_PLACES, "Quantity")
    }
}

data class SaleSessionLineUpdateDto(
    val identity: SessionIdentity,
    @param:JsonProperty("quantity")
    val userProvidedQuantity: BigDecimal,
    val unitId: UUID,
    val unitPriceOverride: BigDecimal,
) {
    init {
        Decimals.requireMaxDecimalPlaces(userProvidedQuantity, QuantityLimits.MAX_USER_QUANTITY_DECIMAL_PLACES, "Quantity")
    }
}

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
    val calculationMethod: PriceAdjustmentMethod,
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

data class SaleSessionVoidDto(
    val reason: String,
)
