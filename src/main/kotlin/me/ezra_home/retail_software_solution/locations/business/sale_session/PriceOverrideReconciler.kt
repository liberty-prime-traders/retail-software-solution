package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LinePricing
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.PriceAdjustmentMethod
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLineUpdateDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SessionIdentity
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentReasonService
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.SystemAdjustmentReason
import me.ezra_home.retail_software_solution.util.business.Decimals
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

private sealed class LineOverrideUpdate {
    object Remove : LineOverrideUpdate()
    data class Upsert(val adjustment: SaleSessionAdjustment) : LineOverrideUpdate()
    object NoChange : LineOverrideUpdate()
}

@Service
class PriceOverrideReconciler(
    private val adjustmentReasonService: AdjustmentReasonService,
) {

    fun reconcile(
        preUpdatePriceOverrides: List<SaleSessionAdjustment>,
        preUpdateSaleSessionLinesByKey: Map<UUID, SaleSessionLine>,
        postUpdateSaleSessionLinesByKey: Map<UUID, SaleSessionLine>,
        updates: List<SaleSessionLineUpdateDto>,
    ): List<SaleSessionAdjustment> {
        if (updates.isEmpty()) return preUpdatePriceOverrides
        val priceOverrideReasonId = adjustmentReasonService.getSystemReasonId(SystemAdjustmentReason.PRICE_OVERRIDE)
        val preUpdateOverridesByLineKey = preUpdatePriceOverrides.associateBy { it.requiredRelatedSaleLineKey() }

        val lineUpdateResults: Map<UUID, LineOverrideUpdate> = updates.associate { saleSessionLineUpdateDto ->
            saleSessionLineUpdateDto.identity.key() to reconcileLineOverride(
                saleSessionLineUpdateDto,
                preUpdateOverridesByLineKey,
                preUpdateSaleSessionLinesByKey,
                postUpdateSaleSessionLinesByKey,
                priceOverrideReasonId,
            )
        }

        val mergedExisting = preUpdatePriceOverrides.mapNotNull { saleSessionAdjustment ->
            val lineKey = saleSessionAdjustment.requiredRelatedSaleLineKey()
            when (val lineOverrideUpdate = lineUpdateResults[lineKey]) {
                is LineOverrideUpdate.Remove -> null
                is LineOverrideUpdate.Upsert -> lineOverrideUpdate.adjustment
                is LineOverrideUpdate.NoChange, null -> saleSessionAdjustment
            }
        }

        val newAdjustments = lineUpdateResults
            .filterKeys { lineKey -> lineKey !in preUpdateOverridesByLineKey }
            .values
            .filterIsInstance<LineOverrideUpdate.Upsert>()
            .map { it.adjustment }

        return mergedExisting + newAdjustments
    }

    // The adjustment stores the typed unit price; its amount is recomputed from the price and the line's quantity on every save
    private fun reconcileLineOverride(
        saleSessionLineUpdateDto: SaleSessionLineUpdateDto,
        preUpdateOverridesByLineKey: Map<UUID, SaleSessionAdjustment>,
        preUpdateSaleSessionLinesByKey: Map<UUID, SaleSessionLine>,
        postUpdateSaleSessionLinesByKey: Map<UUID, SaleSessionLine>,
        priceOverrideReasonId: UUID,
    ): LineOverrideUpdate {
        val lineKey = saleSessionLineUpdateDto.identity.key()
        val postUpdateSaleSessionLine = postUpdateSaleSessionLinesByKey.getValue(lineKey)
        val preUpdateSaleSessionLine = preUpdateSaleSessionLinesByKey.getValue(lineKey)
        val preUpdateOverride = preUpdateOverridesByLineKey[lineKey]

        val unitChanged = preUpdateSaleSessionLine.unitId != postUpdateSaleSessionLine.unitId
        val preUpdateUnitPrice = preUpdateOverride?.value ?: preUpdateSaleSessionLine.unitPrice
        val changedUnitPriceOverride = saleSessionLineUpdateDto.unitPriceOverride?.toBigDecimal()
            ?.takeIf { typedUnitPriceOverride -> Decimals.Scale4.compare(typedUnitPriceOverride, preUpdateUnitPrice) != 0 }
        if (unitChanged && changedUnitPriceOverride == null) {
            return if (preUpdateOverride != null) LineOverrideUpdate.Remove else LineOverrideUpdate.NoChange
        }
        val unitPriceToApply = changedUnitPriceOverride ?: preUpdateOverride?.value ?: return LineOverrideUpdate.NoChange
        return resolveOverrideUpsert(unitPriceToApply, preUpdateOverride, postUpdateSaleSessionLine, priceOverrideReasonId)
    }

    private fun resolveOverrideUpsert(
        unitPrice: BigDecimal,
        preUpdateOverride: SaleSessionAdjustment?,
        postUpdateSaleSessionLine: SaleSessionLine,
        priceOverrideReasonId: UUID,
    ): LineOverrideUpdate {
        val overrideTotal = LinePricing.overrideTotal(unitPrice, postUpdateSaleSessionLine.userProvidedQuantity)
        val totalDifference = postUpdateSaleSessionLine.lineTotal.subtract(overrideTotal)
        if (totalDifference.signum() == 0) {
            return if (preUpdateOverride == null) LineOverrideUpdate.NoChange else LineOverrideUpdate.Remove
        }
        val direction = if (totalDifference.signum() > 0) AdjustmentDirection.DISCOUNT else AdjustmentDirection.SURCHARGE
        if (preUpdateOverride != null && preUpdateOverride.value.compareTo(unitPrice) == 0 && preUpdateOverride.direction == direction) {
            return LineOverrideUpdate.NoChange
        }
        // value and direction are immutable once saved, so a new price or direction replaces the adjustment under a fresh identity
        return LineOverrideUpdate.Upsert(
            SaleSessionAdjustment(
                identity = SessionIdentity.mintFreshIdentity(),
                relatedSaleLineIdentity = postUpdateSaleSessionLine.identity,
                adjustmentReasonId = priceOverrideReasonId,
                direction = direction,
                calculationMethod = PriceAdjustmentMethod.UNIT_PRICE_OVERRIDE,
                value = unitPrice,
                note = null,
                approvedById = null,
            )
        )
    }
}
