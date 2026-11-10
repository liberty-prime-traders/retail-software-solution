package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.util.exceptions.BlockingProblem
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity

data class ReservationShortfall(
    val blockingProblems: List<BlockingProblem>,
    val productName: String,
    val quantityLeftInStock: ExactQuantity,
    val quantityHeld: ExactQuantity,
)
