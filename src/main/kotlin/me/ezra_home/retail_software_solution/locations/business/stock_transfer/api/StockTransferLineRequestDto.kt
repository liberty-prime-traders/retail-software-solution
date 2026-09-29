package me.ezra_home.retail_software_solution.locations.business.stock_transfer.api

data class StockTransferLineRequestDto(
    val additions: List<StockTransferLineInsertDto> = emptyList(),
    val updates: List<StockTransferLineUpdateDto> = emptyList()
)
