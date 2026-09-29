package me.ezra_home.retail_software_solution.locations.business.purchase.api

import java.util.UUID

data class PurchaseDeliveryContext(
    val purchaseId: UUID,
    val supplierId: UUID,
    val purchaseLineById: Map<UUID, PurchaseLineDto>
)
