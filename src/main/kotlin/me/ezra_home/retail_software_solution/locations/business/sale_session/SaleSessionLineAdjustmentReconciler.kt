package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLineUpdateDto
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentReasonService
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.SystemAdjustmentReason
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SaleSessionLineAdjustmentReconciler(
    private val priceOverrideReconciler: PriceOverrideReconciler,
    private val adjustmentReasonService: AdjustmentReasonService,
) {

    fun reconcile(
        preUpdateSaleAdjustments: List<SaleSessionAdjustment>,
        preUpdateSaleSessionLinesByKey: Map<UUID, SaleSessionLine>,
        postUpdateSaleSessionLinesByKey: Map<UUID, SaleSessionLine>,
        updates: List<SaleSessionLineUpdateDto>,
    ): List<SaleSessionAdjustment> {
        val priceOverrideReasonId = adjustmentReasonService.getSystemReasonId(SystemAdjustmentReason.PRICE_OVERRIDE)
        val (priceOverrides, otherAdjustments) = preUpdateSaleAdjustments.partition { it.isPriceOverride(priceOverrideReasonId) }

        val unitChangedLineKeys = updates
            .filter { update ->
                preUpdateSaleSessionLinesByKey[update.identity.key()]?.unitId !=
                        postUpdateSaleSessionLinesByKey[update.identity.key()]?.unitId
            }
            .map { it.identity.key() }
            .toSet()

        val survivingOtherAdjustments = otherAdjustments.filter { saleSessionAdjustment ->
            val lineKey = saleSessionAdjustment.relatedSaleLineIdentity?.key()
            lineKey == null || lineKey !in unitChangedLineKeys
        }

        val reconciledPriceOverrides = priceOverrideReconciler.reconcile(
            preUpdatePriceOverrides = priceOverrides,
            preUpdateSaleSessionLinesByKey = preUpdateSaleSessionLinesByKey,
            postUpdateSaleSessionLinesByKey = postUpdateSaleSessionLinesByKey,
            updates = updates,
        )

        return survivingOtherAdjustments + reconciledPriceOverrides
    }
}
