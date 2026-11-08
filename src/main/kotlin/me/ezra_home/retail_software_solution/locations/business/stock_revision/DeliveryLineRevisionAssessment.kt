package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.locations.business.delivery.api.ValidatedDeliveryLineRevisionDto
import me.ezra_home.retail_software_solution.util.exceptions.BlockingProblem
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import java.util.UUID

data class DeliveryLineRevisionAssessment(
    val validatedDeliveryLineRevision: ValidatedDeliveryLineRevisionDto,
    val quantityBreakdown: QuantityBreakdown,
    val replayResult: ReplayResult,
    val reservationShortfall: ReservationShortfall?,
    val replacementStockEntryId: UUID? = null,
) {
    val reservationProblems: List<BlockingProblem> = reservationShortfall?.blockingProblems ?: emptyList()
    val blockingProblems: List<BlockingProblem> = buildList {
        addAll(validatedDeliveryLineRevision.problems)
        addAll(replayResult.problems)
        addAll(reservationProblems)
    }
}
