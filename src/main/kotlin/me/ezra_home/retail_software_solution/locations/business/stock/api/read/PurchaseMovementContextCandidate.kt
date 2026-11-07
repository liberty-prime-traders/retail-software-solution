package me.ezra_home.retail_software_solution.locations.business.stock.api.read

import java.util.UUID

data class PurchaseMovementContextCandidate(
    val stockMovementId: UUID,
    val purchaseReferenceNumber: String,
)
