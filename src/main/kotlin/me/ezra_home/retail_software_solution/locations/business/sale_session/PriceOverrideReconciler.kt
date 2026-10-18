package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LinePricing
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLineUpdateDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SessionIdentity
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentReasonService
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.SystemAdjustmentReason
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.PriceAdjustmentMethod
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.quantity.QuantityLimits
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
        val preUpdateOverridesByLineKey = preUpdatePriceOverrides.associateBy { it.relatedSaleLineIdentity!!.key() }

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
            val lineKey = saleSessionAdjustment.relatedSaleLineIdentity!!.key()
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

    // An override is the difference between the line's computed total and typed price x quantity; the typed price is never stored
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
        val overrideChanged = unitPriceOverrideChanged(
            saleSessionLineUpdateDto.unitPriceOverride, preUpdateOverride, preUpdateSaleSessionLine
        )
        if (unitChanged && !overrideChanged) {
            return if (preUpdateOverride != null) LineOverrideUpdate.Remove else LineOverrideUpdate.NoChange
        }
        if (overrideChanged) {
            // Checked here, not in the request's init: the response's unitPriceOverride is a derived price that can have 4 decimals,
            // and the client echoes it back unchanged on every edit. Only a price that differs from it is a typed override.
            Decimals.requireMaxDecimalPlaces(
                saleSessionLineUpdateDto.unitPriceOverride, QuantityLimits.MAX_PRICE_OVERRIDE_DECIMAL_PLACES, "Price override"
            )
        }
        if (overrideChanged || preUpdateOverride != null) {
            return resolveOverrideUpsert(
                saleSessionLineUpdateDto.unitPriceOverride,
                preUpdateOverride,
                postUpdateSaleSessionLine,
                priceOverrideReasonId,
            )
        }
        return LineOverrideUpdate.NoChange
    }

    private fun unitPriceOverrideChanged(
        submittedUnitPriceOverride: BigDecimal,
        preUpdateOverride: SaleSessionAdjustment?,
        preUpdateSaleSessionLine: SaleSessionLine,
    ): Boolean {
        val preUpdateUnitPriceOverride = preUpdateOverride?.let {
            val preUpdateOverrideTotal = when (it.direction) {
                AdjustmentDirection.DISCOUNT -> preUpdateSaleSessionLine.lineTotal - it.value
                AdjustmentDirection.SURCHARGE -> preUpdateSaleSessionLine.lineTotal + it.value
                AdjustmentDirection.BOTH -> throw IllegalStateException("PRICE_OVERRIDE adjustment cannot have direction BOTH")
            }
            Decimals.Scale4.divide(preUpdateOverrideTotal, preUpdateSaleSessionLine.userProvidedQuantity)
        } ?: preUpdateSaleSessionLine.unitPrice
        return Decimals.Scale4.compare(submittedUnitPriceOverride, preUpdateUnitPriceOverride) != 0
    }

    private fun resolveOverrideUpsert(
        submittedUnitPriceOverride: BigDecimal,
        preUpdateOverride: SaleSessionAdjustment?,
        postUpdateSaleSessionLine: SaleSessionLine,
        priceOverrideReasonId: UUID,
    ): LineOverrideUpdate {
        val overrideTotal = LinePricing.overrideTotal(submittedUnitPriceOverride, postUpdateSaleSessionLine.userProvidedQuantity)
        val totalDifference = postUpdateSaleSessionLine.lineTotal.subtract(overrideTotal)
        if (totalDifference.signum() == 0) {
            return if (preUpdateOverride == null) LineOverrideUpdate.NoChange else LineOverrideUpdate.Remove
        }
        val direction = if (totalDifference.signum() > 0) AdjustmentDirection.DISCOUNT else AdjustmentDirection.SURCHARGE
        val adjustment = preUpdateOverride?.copy(value = totalDifference.abs(), direction = direction)
            ?: SaleSessionAdjustment(
                identity = SessionIdentity.mintFreshIdentity(),
                relatedSaleLineIdentity = postUpdateSaleSessionLine.identity,
                adjustmentReasonId = priceOverrideReasonId,
                direction = direction,
                calculationMethod = PriceAdjustmentMethod.FIXED_LINE_TOTAL,
                value = totalDifference.abs(),
                note = null,
                approvedById = null,
            )
        return LineOverrideUpdate.Upsert(adjustment)
    }
}
