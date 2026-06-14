package me.ezra_home.retail_software_solution.locations.business.stock_transfer.api

import java.util.UUID

data class StockTransferCreateDto(
    val destinationLocationId: UUID,
    val notes: String?
)
