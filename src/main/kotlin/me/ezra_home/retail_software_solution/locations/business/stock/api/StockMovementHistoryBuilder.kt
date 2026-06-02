package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.StockMovementReasonFetcher
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueFetcher
import me.ezra_home.retail_software_solution.util.business.Decimals
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
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
        val after = OffsetDateTime.now().minus(60, ChronoUnit.DAYS)
        val movements = stockMovementRepository.findByLocationProductIdAndCreatedOnAfter(locationProductId, after)
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
                    movement.conversionFactor, unitCode, baseUnitCode
                ),
                reason = movement.reasonId?.let { reasonNamesById[it] }
            )
        }
    }

    private fun conversionDescription(
        unitId: UUID,
        baseUnitId: UUID,
        recordedFactor: BigDecimal,
        unitCode: String,
        baseUnitCode: String
    ): String? {
        if (unitId == baseUnitId) return null
        val currentFactor = try {
            unitConversionGraphFacade.getFactor(unitId, baseUnitId)
        } catch (_: Exception) {
            return null
        }
        val formatedRecordedFactor = Decimals.stripZeroesAndRound(recordedFactor)
        return if (currentFactor.compareTo(recordedFactor) != 0) "1 $unitCode = $formatedRecordedFactor $baseUnitCode" else null
    }
}
