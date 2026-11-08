package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.locations.business.delivery.api.ValidatedDeliveryLineRevisionDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.revision.ConsumerReplayPlan
import me.ezra_home.retail_software_solution.locations.business.stock_revision.api.RevisionPreviewItemDto
import me.ezra_home.retail_software_solution.locations.business.stock_revision.api.RevisionPreviewSeverity
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.business.DisplayFormatters
import me.ezra_home.retail_software_solution.util.exceptions.BlockingProblem
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import java.math.BigDecimal
import java.util.UUID

object RevisionPreviewStoryWriter {

    fun write(
        replayResult: ReplayResult,
        reservationProblems: List<BlockingProblem>,
        quantityBreakdown: QuantityBreakdown
    ): List<RevisionPreviewItemDto> {
        if (replayResult.problems.isNotEmpty()) {
            return replayResult.problems.map { blockingItem(it) }.sortedBy { it.documentReference }
        }
        val blockingItems = reservationProblems.map { blockingItem(it) }
        val costChangeItems = replayResult.consumerReplayPlans
            .filter { it.oldCost.compareTo(it.newCost) != 0 }
            .map { costChangeItem(it, quantityBreakdown) }
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

    private fun costChangeItem(
        consumerReplayPlan: ConsumerReplayPlan,
        quantityBreakdown: QuantityBreakdown
    ): RevisionPreviewItemDto {
        val fifoConsumptionRequest = consumerReplayPlan.fifoConsumptionRequest
        val consumerLabel = StockConsumerLabel.of(fifoConsumptionRequest.movementType)
        val quantityText = quantityBreakdown.render(fifoConsumptionRequest.storageQuantity)
        val oldCostText = DisplayFormatters.formatCurrency(consumerReplayPlan.oldCost)
        val newCostText = DisplayFormatters.formatCurrency(consumerReplayPlan.newCost)
        val isSale = fifoConsumptionRequest.movementType == MovementType.SALE
        val netProfitChange = if (isSale) consumerReplayPlan.oldCost.subtract(consumerReplayPlan.newCost) else null
        val effect = if (isSale) {
            "Cost of goods sold goes from $oldCostText to $newCostText for the $quantityText taken from this delivery."
        } else {
            "The cost of the $quantityText taken from this delivery goes from $oldCostText to $newCostText."
        }
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
