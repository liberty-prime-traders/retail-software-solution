package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.locations.business.delivery.api.ValidatedDeliveryLineRevisionDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.revision.ConsumerReplayPlan
import me.ezra_home.retail_software_solution.locations.business.stock_revision.api.AdjustmentCostChange
import me.ezra_home.retail_software_solution.locations.business.stock_revision.api.RevisionPreviewItemDto
import me.ezra_home.retail_software_solution.locations.business.stock_revision.api.RevisionPreviewSeverity
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.business.DisplayFormatters
import me.ezra_home.retail_software_solution.util.exceptions.BlockingProblem
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import java.math.BigDecimal
import java.util.UUID

object RevisionPreviewStoryWriter {

    fun write(
        replayResult: ReplayResult,
        reservationProblems: List<BlockingProblem>,
        quantityBreakdown: QuantityBreakdown,
        replacementStockEntryId: UUID?,
        adjustmentCostChangeByReference: Map<String, AdjustmentCostChange>
    ): List<RevisionPreviewItemDto> {
        if (replayResult.problems.isNotEmpty()) {
            return replayResult.problems.map { blockingItem(it) }.sortedBy { it.documentReference }
        }
        val blockingItems = reservationProblems.map { blockingItem(it) }
        val costChangeItems = replayResult.consumerReplayPlans
            .filter { it.oldCost.compareTo(it.newCost) != 0 }
            .map { costChangeItem(it, quantityBreakdown, replacementStockEntryId, adjustmentCostChangeByReference) }
        return (blockingItems + costChangeItems).sortedWith(compareBy({ it.severity }, { it.documentReference }))
    }

    fun changeSummary(
        validatedDeliveryLineRevision: ValidatedDeliveryLineRevisionDto,
        quantityBreakdown: QuantityBreakdown
    ): String {
        val before = lineText(
            quantityBreakdown,
            validatedDeliveryLineRevision.revisedUnitId,
            validatedDeliveryLineRevision.revisedUserProvidedQuantityDelivered,
            validatedDeliveryLineRevision.revisedUnitCost
        )
        val after = lineText(
            quantityBreakdown,
            validatedDeliveryLineRevision.unitId,
            validatedDeliveryLineRevision.userProvidedQuantityDelivered,
            validatedDeliveryLineRevision.unitCost
        )
        return "$before becomes $after."
    }

    private fun lineText(
        quantityBreakdown: QuantityBreakdown,
        unitId: UUID,
        userProvidedQuantity: BigDecimal,
        unitCost: BigDecimal
    ): String {
        val quantityText = quantityBreakdown.renderUserProvided(userProvidedQuantity, unitId)
        val unitCostText = DisplayFormatters.formatCurrency(unitCost)
        return "$quantityText at $unitCostText"
    }

    fun blockingItem(blockingProblem: BlockingProblem): RevisionPreviewItemDto {
        return RevisionPreviewItemDto(
            severity = RevisionPreviewSeverity.BLOCKS_SAVE,
            documentReference = blockingProblem.documentReference,
            description = blockingProblem.description,
            effect = blockingProblem.effect,
        )
    }

    // The consumer drew from the replaced layer; whatever the replacement cannot cover is drawn from other layers.
    private fun whyTheCostMoved(
        consumerReplayPlan: ConsumerReplayPlan,
        quantityBreakdown: QuantityBreakdown,
        replacementStockEntryId: UUID?
    ): String? {
        if (replacementStockEntryId == null) return null
        val (takesFromReplacement, takesFromOtherDeliveries) =
            consumerReplayPlan.plannedTakes.partition { it.stockEntryId == replacementStockEntryId }
        if (takesFromOtherDeliveries.isEmpty()) return "All of it is priced at this delivery's revised cost."
        val quantityFromOtherDeliveries = quantityBreakdown.render(
            takesFromOtherDeliveries.fold(ExactQuantity.ZERO) { total, take -> total + take.storageQuantity }
        )
        if (takesFromReplacement.isEmpty()) {
            return "This delivery has none of it left, so all of it is priced from other deliveries at their own cost."
        }
        val quantityFromReplacement = quantityBreakdown.render(
            takesFromReplacement.fold(ExactQuantity.ZERO) { total, take -> total + take.storageQuantity }
        )
        return "This delivery covers only $quantityFromReplacement of it at its revised cost; the other " +
            "$quantityFromOtherDeliveries is priced from other deliveries at their own cost."
    }

    private fun costChangeItem(
        consumerReplayPlan: ConsumerReplayPlan,
        quantityBreakdown: QuantityBreakdown,
        replacementStockEntryId: UUID?,
        adjustmentCostChangeByReference: Map<String, AdjustmentCostChange>
    ): RevisionPreviewItemDto {
        val fifoConsumptionRequest = consumerReplayPlan.fifoConsumptionRequest
        val consumerLabel = StockConsumerLabel.of(fifoConsumptionRequest.movementType)
        val quantityText = quantityBreakdown.render(fifoConsumptionRequest.storageQuantity)
        val oldCostText = DisplayFormatters.formatCurrency(consumerReplayPlan.oldCost)
        val newCostText = DisplayFormatters.formatCurrency(consumerReplayPlan.newCost)
        val isSale = fifoConsumptionRequest.movementType == MovementType.SALE
        val netProfitChange = if (isSale) {
            consumerReplayPlan.oldCost.subtract(consumerReplayPlan.newCost)
        } else {
            adjustmentCostChangeByReference[fifoConsumptionRequest.documentReference]?.netProfitChange()
        }
        val costChangeText = if (isSale) {
            "Cost of goods sold goes from $oldCostText to $newCostText for the $quantityText taken from this delivery."
        } else {
            "The cost of the $quantityText taken from this delivery goes from $oldCostText to $newCostText."
        }
        val effect = listOfNotNull(
            costChangeText, whyTheCostMoved(consumerReplayPlan, quantityBreakdown, replacementStockEntryId)
        ).joinToString(" ")
        return RevisionPreviewItemDto(
            severity = RevisionPreviewSeverity.NOTE,
            documentReference = listOfNotNull(
                fifoConsumptionRequest.documentReference, fifoConsumptionRequest.detailReference
            ).joinToString("/"),
            description = "$consumerLabel that took $quantityText from this delivery.",
            effect = effect,
            netProfitChange = netProfitChange,
        )
    }
}
