package me.ezra_home.retail_software_solution.locations.business.stock

import java.util.UUID

interface PurchaseMovementContextProjection {
    val stockMovementId: UUID
    val purchaseReferenceNumber: String
}
