package me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api

import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.StockMovementReasonCache
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.StockMovementReasonMapper
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class StockMovementReasonFetcher(
    private val cache: StockMovementReasonCache,
    private val mapper: StockMovementReasonMapper
) {

    fun getAll(): Collection<StockMovementReasonResponseDto> =
        cache.getAll().map { mapper.toResponseDto(it) }

    fun getNamesById(): Map<UUID, String> =
        cache.getAll().associate { it.id to it.name }
}
