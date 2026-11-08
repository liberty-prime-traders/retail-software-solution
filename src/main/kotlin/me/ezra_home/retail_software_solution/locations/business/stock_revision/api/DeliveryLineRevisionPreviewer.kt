package me.ezra_home.retail_software_solution.locations.business.stock_revision.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock_revision.AdjustmentCostChangeResolver
import me.ezra_home.retail_software_solution.locations.business.stock_revision.ConsumerCostChange
import me.ezra_home.retail_software_solution.locations.business.stock_revision.DeliveryLineRevisionAssessor
import me.ezra_home.retail_software_solution.locations.business.stock_revision.RevisionGuidanceWriter
import me.ezra_home.retail_software_solution.locations.business.stock_revision.RevisionLedgerPreviewWriter
import me.ezra_home.retail_software_solution.locations.business.stock_revision.RevisionPreviewStoryWriter
import org.springframework.stereotype.Service

// Takes no lock, so a preview can differ from the save, which assesses again under the product lock.
@Service
@TransactionalOnLocationSchema(readOnly = true)
class DeliveryLineRevisionPreviewer(
    private val deliveryLineRevisionAssessor: DeliveryLineRevisionAssessor,
    private val revisionLedgerPreviewWriter: RevisionLedgerPreviewWriter,
    private val adjustmentCostChangeResolver: AdjustmentCostChangeResolver,
) {

    fun preview(deliveryLineRevisionRequest: DeliveryLineRevisionRequest): DeliveryLineRevisionPreviewDto {
        val assessment = deliveryLineRevisionAssessor.assess(deliveryLineRevisionRequest)
        val validatedDeliveryLineRevision = assessment.validatedDeliveryLineRevision
        val consumerReplayPlans = assessment.replayResult.consumerReplayPlans
        val adjustmentCostChangeByReference = if (validatedDeliveryLineRevision.problems.isEmpty()) {
            adjustmentCostChangeResolver.byAdjustmentReference(ConsumerCostChange.ofPlans(consumerReplayPlans))
        } else {
            emptyMap()
        }
        val transactions = if (validatedDeliveryLineRevision.problems.isNotEmpty()) {
            validatedDeliveryLineRevision.problems.map(RevisionPreviewStoryWriter::blockingItem)
        } else {
            RevisionPreviewStoryWriter.write(
                assessment.replayResult, assessment.reservationProblems, assessment.quantityBreakdown,
                assessment.replacementStockEntryId, adjustmentCostChangeByReference
            )
        }
        val ledgerEntries = if (assessment.blockingProblems.isEmpty()) {
            revisionLedgerPreviewWriter.write(
                validatedDeliveryLineRevision, consumerReplayPlans, adjustmentCostChangeByReference.values
            )
        } else {
            emptyList()
        }
        return DeliveryLineRevisionPreviewDto(
            revisedDeliveryLineReferenceNumber = validatedDeliveryLineRevision.revisedLineReferenceNumber,
            changeSummary = RevisionPreviewStoryWriter.changeSummary(
                validatedDeliveryLineRevision, assessment.quantityBreakdown
            ),
            transactions = transactions,
            guidance = RevisionGuidanceWriter.write(assessment),
            ledgerEntries = ledgerEntries,
        )
    }
}
