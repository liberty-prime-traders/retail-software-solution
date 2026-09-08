package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.StockMovementReasonFetcher
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueFetcher
import me.ezra_home.retail_software_solution.util.business.ConversionRatio
import me.ezra_home.retail_software_solution.util.business.Decimals
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class StockMovementHistoryBuilder(
    private val stockMovementRepository: StockMovementRepository,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val unitValueFetcher: UnitValueFetcher,
    private val unitConversionGraphFacade: UnitConversionGraphFacade,
    private val stockMovementReasonFetcher: StockMovementReasonFetcher
) {

    fun build(locationProductId: UUID): Collection<StockMovementResponse> {
        val movements = stockMovementRepository.findByLocationProductId(locationProductId, Pageable.ofSize(100))
        if (movements.isEmpty()) return emptyList()

        val baseUnitId = locationProductDataFetcher.getBaseUnitIds(listOf(locationProductId))[locationProductId]!!
        val unitCodesById = unitValueFetcher.getAllUnitValues().associate { it.id to it.code }
        val baseUnitCode = unitCodesById[baseUnitId] ?: ""
        val reasonNamesById = stockMovementReasonFetcher.getNamesById()

        return movements.map { movement ->
            val unitCode = unitCodesById[movement.unitId] ?: ""
            StockMovementResponse(
                id = movement.id!!,
                movementType = movement.movementType,
                locationProductId = movement.locationProductId,
                externalReferenceNumber = movement.externalReferenceNumber,
                quantityMoved = "${Decimals.stripZeroesAndRound(movement.movedQuantity)} $unitCode",
                newQuantity = "${Decimals.stripZeroesAndRound(movement.remainingQuantity)} $baseUnitCode",
                recordedOn = movement.createdOn!!.toInstant(),
                conversionDriftNote = conversionDescription(
                    movement.unitId, baseUnitId,
                    movement.conversionRatio(), unitCode, baseUnitCode
                ),
                reason = movement.reasonId?.let { reasonNamesById[it] }
            )
        }
    }

    private fun conversionDescription(
        unitId: UUID,
        baseUnitId: UUID,
        recordedRatio: ConversionRatio,
        unitCode: String,
        baseUnitCode: String
    ): String? {
        if (unitId == baseUnitId) return null
        val currentRatio = try {
            unitConversionGraphFacade.getRatio(unitId, baseUnitId)
        } catch (_: Exception) {
            return null
        }
        if (recordedRatio.isEquivalentTo(currentRatio)) return null
        val formatedRecordedFactor = Decimals.stripZeroesAndRound(recordedRatio.factor())
        return "1 $unitCode = $formatedRecordedFactor $baseUnitCode"
    }
}
