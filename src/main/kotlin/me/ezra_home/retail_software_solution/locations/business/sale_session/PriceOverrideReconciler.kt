package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLineUpdateDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SessionIdentity
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentReasonService
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.SystemAdjustmentReason
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.CalculationMethod
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
            when (it.direction) {
                AdjustmentDirection.DISCOUNT -> preUpdateSaleSessionLine.unitPrice - it.value
                AdjustmentDirection.SURCHARGE -> preUpdateSaleSessionLine.unitPrice + it.value
                AdjustmentDirection.BOTH -> throw IllegalStateException("PRICE_OVERRIDE adjustment cannot have direction BOTH")
            }
        } ?: preUpdateSaleSessionLine.unitPrice
        return submittedUnitPriceOverride.compareTo(preUpdateUnitPriceOverride) != 0
    }

    private fun resolveOverrideUpsert(
        submittedUnitPriceOverride: BigDecimal,
        preUpdateOverride: SaleSessionAdjustment?,
        postUpdateSaleSessionLine: SaleSessionLine,
        priceOverrideReasonId: UUID,
    ): LineOverrideUpdate {
        val priceDifference = postUpdateSaleSessionLine.unitPrice.subtract(submittedUnitPriceOverride)
        if (priceDifference.signum() == 0) {
            return if (preUpdateOverride == null) LineOverrideUpdate.NoChange else LineOverrideUpdate.Remove
        }
        val direction = if (priceDifference.signum() > 0) AdjustmentDirection.DISCOUNT else AdjustmentDirection.SURCHARGE
        val adjustment = preUpdateOverride?.copy(value = priceDifference.abs(), direction = direction)
            ?: SaleSessionAdjustment(
                identity = SessionIdentity.mintFreshIdentity(),
                relatedSaleLineIdentity = postUpdateSaleSessionLine.identity,
                adjustmentReasonId = priceOverrideReasonId,
                direction = direction,
                calculationMethod = CalculationMethod.FIXED_VALUE,
                value = priceDifference.abs(),
                note = null,
                approvedById = null,
            )
        return LineOverrideUpdate.Upsert(adjustment)
    }
}
