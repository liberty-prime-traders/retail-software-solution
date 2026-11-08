package me.ezra_home.retail_software_solution.locations.business.stock_revision.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock_revision.DeliveryLineRevisionAssessor
import me.ezra_home.retail_software_solution.locations.business.stock_revision.RevisionGuidanceWriter
import me.ezra_home.retail_software_solution.locations.business.stock_revision.RevisionPreviewStoryWriter
import org.springframework.stereotype.Service

// Takes no lock, so a preview can differ from the save, which assesses again under the product lock.
@Service
@TransactionalOnLocationSchema(readOnly = true)
class DeliveryLineRevisionPreviewer(
    private val deliveryLineRevisionAssessor: DeliveryLineRevisionAssessor,
) {

    fun preview(deliveryLineRevisionRequest: DeliveryLineRevisionRequest): DeliveryLineRevisionPreviewDto {
        val assessment = deliveryLineRevisionAssessor.assess(deliveryLineRevisionRequest)
        val validatedDeliveryLineRevision = assessment.validatedDeliveryLineRevision
        val transactions = if (validatedDeliveryLineRevision.problems.isNotEmpty()) {
            validatedDeliveryLineRevision.problems.map(RevisionPreviewStoryWriter::blockingItem)
        } else {
            RevisionPreviewStoryWriter.write(
                assessment.replayResult, assessment.reservationProblems, assessment.quantityBreakdown
            )
        }
        return DeliveryLineRevisionPreviewDto(
            revisedDeliveryLineReferenceNumber = validatedDeliveryLineRevision.revisedLineReferenceNumber,
            changeSummary = RevisionPreviewStoryWriter.changeSummary(
                validatedDeliveryLineRevision, assessment.quantityBreakdown
            ),
            transactions = transactions,
            guidance = RevisionGuidanceWriter.write(assessment),
            ledgerEntries = emptyList(),
        )
    }
}
