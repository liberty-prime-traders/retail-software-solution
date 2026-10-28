package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.PriceAdjustmentMethod
import java.math.BigDecimal
import java.util.UUID

data class SaleSessionAdjustment(
    val identity: SessionIdentity,
    val relatedSaleLineIdentity: SessionIdentity?,
    val adjustmentReasonId: UUID,
    val direction: AdjustmentDirection,
    val calculationMethod: PriceAdjustmentMethod,
    val value: BigDecimal,
    val note: String?,
    val approvedById: UUID?,
) {
    fun isPriceOverride(priceOverrideReasonId: UUID): Boolean =
        adjustmentReasonId == priceOverrideReasonId && relatedSaleLineIdentity != null

    fun requiredRelatedSaleLineKey(): UUID = relatedSaleLineIdentity?.key()
        ?: error("Sale session adjustment ${identity.key()} is not tied to a sale line")
}
