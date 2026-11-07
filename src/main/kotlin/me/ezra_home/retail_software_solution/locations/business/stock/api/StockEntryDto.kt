package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import java.math.BigDecimal
import java.util.UUID

data class StockEntryDto(
    val id: UUID,
    val referenceNumber: String,
    val locationProductId: UUID,
    val sourceType: StockItemSource?,
    val documentReference: String?,
    val detailReference: String?,
    val storageQuantityRemaining: BigDecimal,
    val costRemaining: BigDecimal,
    val status: StockEntryStatus,
) {
    companion object {
        fun of(stockEntryEntity: StockEntryEntity) = StockEntryDto(
            id = stockEntryEntity.id!!,
            referenceNumber = stockEntryEntity.requiredReference(),
            locationProductId = stockEntryEntity.locationProductId,
            sourceType = stockEntryEntity.sourceType,
            documentReference = stockEntryEntity.documentReference,
            detailReference = stockEntryEntity.detailReference,
            storageQuantityRemaining = stockEntryEntity.storageQuantityRemaining,
            costRemaining = stockEntryEntity.costRemaining,
            status = stockEntryEntity.status,
        )
    }
}
