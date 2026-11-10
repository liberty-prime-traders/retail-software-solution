package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.locations.business.stock_revision.api.GuidanceParagraphDto
import me.ezra_home.retail_software_solution.locations.business.stock_revision.api.TextSegmentDto
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.exceptions.BlockingProblem
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown

object RevisionGuidanceWriter {

    private val possibleExplanations = listOf(
        "Check which of these is true:",
        "• You received more than you have entered: check the delivery note and enter the real quantity.",
        "• One of these sales was recorded incorrectly: correct or void it first, then come back to this change.",
        "• There was stock that came from another delivery that was never recorded:" +
                " record that delivery first so it can cover the stranded sales."
    ).map { plainParagraphOf(it) }

    fun write(assessment: DeliveryLineRevisionAssessment): List<GuidanceParagraphDto> {
        if (assessment.replayResult.problems.isNotEmpty()) {
            return if (onlySalesAreUncovered(assessment)) uncoveredRecordsGuidance(assessment) else emptyList()
        }
        val reservationShortfall = assessment.reservationShortfall
        if (reservationShortfall != null) {
            return listOf(draftSalesGuidance(reservationShortfall, assessment.quantityBreakdown))
        }
        return emptyList()
    }

    private fun onlySalesAreUncovered(assessment: DeliveryLineRevisionAssessment): Boolean {
        val uncoveredMovementTypes = assessment.replayResult.movementTypesOfUncoveredConsumers
        return uncoveredMovementTypes.isNotEmpty() && uncoveredMovementTypes.all { it == MovementType.SALE }
    }

    private fun uncoveredRecordsGuidance(assessment: DeliveryLineRevisionAssessment): List<GuidanceParagraphDto> {
        val replacementStorageQuantity = assessment.validatedDeliveryLineRevision.replacementStorageQuantity
        val quantityTheRecordsTook = assessment.replayResult.totalTakenByConsumers
        val quantityBreakdown = assessment.quantityBreakdown
        val explanation = GuidanceParagraphDto(
            buildList {
                addAll(referenceSegments(assessment.replayResult.problems))
                add(TextSegmentDto(" cannot be fully covered. Together, the records that drew from this delivery "))
                add(TextSegmentDto("took "))
                add(TextSegmentDto(quantityBreakdown.render(quantityTheRecordsTook), emphasis = true))
                add(TextSegmentDto(", and your change leaves "))
                add(TextSegmentDto(quantityBreakdown.render(replacementStorageQuantity), emphasis = true))
                add(TextSegmentDto("."))
            }
        )
        if (replacementStorageQuantity > ExactQuantity.ZERO) return listOf(explanation) + possibleExplanations
        val voidingFirst = plainParagraphOf("If this delivery never happened, the sales above must be voided first.")
        return buildList {
            add(explanation)
            addAll(possibleExplanations)
            add(voidingFirst)
        }
    }

    private fun draftSalesGuidance(
        reservationShortfall: ReservationShortfall,
        quantityBreakdown: QuantityBreakdown
    ): GuidanceParagraphDto {
        val isOneDraft = reservationShortfall.blockingProblems.size == 1
        val heldBetweenThem = if (isOneDraft) "" else " between them"
        val them = if (isOneDraft) "it" else "them"
        val drafts = if (isOneDraft) "that draft" else "those drafts"
        return GuidanceParagraphDto(
            buildList {
                add(TextSegmentDto(if (isOneDraft) "Draft sale " else "Draft sales "))
                addAll(referenceSegments(reservationShortfall.blockingProblems))
                add(TextSegmentDto(if (isOneDraft) " holds " else " hold "))
                add(TextSegmentDto(quantityBreakdown.render(reservationShortfall.quantityHeld), emphasis = true))
                add(TextSegmentDto(" of ${reservationShortfall.productName}$heldBetweenThem, but your change would "))
                add(TextSegmentDto("leave only "))
                add(TextSegmentDto(quantityBreakdown.render(reservationShortfall.quantityLeftInStock), emphasis = true))
                add(TextSegmentDto(" in stock. Nothing has been sold on $them yet. Reduce or remove the product on "))
                add(TextSegmentDto("$drafts, or check that the quantity you are updating on this delivery is right."))
            }
        )
    }

    private fun plainParagraphOf(text: String) = GuidanceParagraphDto(listOf(TextSegmentDto(text)))

    private fun referenceSegments(blockingProblems: List<BlockingProblem>): List<TextSegmentDto> {
        return blockingProblems.map { it.documentReference }.sorted().flatMapIndexed { index, documentReference ->
            val separator = if (index == 0) emptyList() else listOf(TextSegmentDto(", "))
            separator + TextSegmentDto(documentReference, emphasis = true)
        }
    }
}
