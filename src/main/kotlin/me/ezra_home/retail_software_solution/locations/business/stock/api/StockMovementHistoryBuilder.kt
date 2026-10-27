package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.StockMovementReasonFetcher
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraph
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueFetcher
import me.ezra_home.retail_software_solution.util.business.DisplayFormatters
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
class StockMovementHistoryBuilder(
    private val stockMovementRepository: StockMovementRepository,
    private val unitValueFetcher: UnitValueFetcher,
    private val unitConversionGraphFacade: UnitConversionGraphFacade,
    private val stockMovementReasonFetcher: StockMovementReasonFetcher
) {

    fun build(locationProductId: UUID): Collection<StockMovementResponse> {
        val movements = stockMovementRepository.findByLocationProductId(locationProductId, Pageable.ofSize(100))
        if (movements.isEmpty()) return emptyList()

        val unitNamesById = unitValueFetcher.getUnitNamesById()
        val unitConversionGraph = unitConversionGraphFacade.getOrLoad()
        val reasonNamesById = stockMovementReasonFetcher.getNamesById()

        return movements.map { movement ->
            StockMovementResponse(
                id = movement.id!!,
                movementType = movement.movementType,
                locationProductId = movement.locationProductId,
                documentReference = movement.documentReference,
                detailReference = movement.detailReference,
                quantityMoved = movement.storageMovedBreakdownText,
                quantityEntered = "entered ${movement.wholeLineQuantityText}",
                newQuantity = movement.storageRemainingBreakdownText,
                stockEntryId = movement.stockEntryId,
                recordedOn = movement.requiredCreatedOn().toInstant(),
                conversionDriftNote = ratioChangeNote(movement, unitNamesById, unitConversionGraph),
                reason = movement.reasonId?.let { reasonNamesById[it] }
            )
        }
    }

    // Shown only when the unit's size differs from the one captured on the movement; a deleted unit has nothing to compare
    private fun ratioChangeNote(
        movement: StockMovementEntity,
        unitNamesById: Map<UUID, String>,
        unitConversionGraph: UnitConversionGraph
    ): String? {
        val unitName = unitNamesById[movement.unitId] ?: return null
        val currentStorageUnitsPerUserProvidedUnit = try {
            unitConversionGraph.getSizeInRoot(movement.unitId)
        } catch (_: Exception) {
            return null
        }
        if (currentStorageUnitsPerUserProvidedUnit == movement.storageUnitsPerUserProvidedUnit) return null
        val storageUnitName = unitNamesById[unitConversionGraph.getStorageUnitId(movement.unitId)] ?: return null
        val recordedFactor = BigDecimal.valueOf(movement.storageUnitsPerUserProvidedUnit)
        return DisplayFormatters.pluralize("1 $unitName was ${movement.storageUnitsPerUserProvidedUnit} $storageUnitName", recordedFactor)
    }
}
