package me.ezra_home.retail_software_solution.locations.business.purchase

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductService
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductUnitRequestDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineCreateDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineUpdateDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseUpdateDto
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

data class LineUpdateResult(
    val toDelete: List<PurchaseLineEntity>,
    val toUpdate: List<PurchaseLineEntity>,
    val toCreate: List<PurchaseLineEntity>,
    val resultingLines: List<PurchaseLineEntity>
)

@Service
class PurchaseLinesResolver(
    private val purchaseLineRepository: PurchaseLineRepository,
    private val locationProductService: LocationProductService,
    private val locationProductDataFetcher: LocationProductDataFetcher
) {

    fun detanglePurchaseLines(purchaseId: UUID, purchaseUpdateDto: PurchaseUpdateDto): LineUpdateResult {
        val existingLines = purchaseLineRepository.findByPurchaseIdIn(listOf(purchaseId))
        val existingLinesById = existingLines.associateBy { it.id!! }

        val toCreate = computeAdditions(purchaseId, purchaseUpdateDto.linesToAdd)
        val (toDelete, toUpdate) = computeUpdates(purchaseUpdateDto.linesToUpdate, existingLinesById)

        val deletedIds = toDelete.mapTo(HashSet()) { it.id }
        val updatedIds = toUpdate.mapTo(HashSet()) { it.id }
        val resultingLines = existingLines.filter { it.id !in deletedIds && it.id !in updatedIds } + toUpdate + toCreate

        return LineUpdateResult(toDelete, toUpdate, toCreate, resultingLines)
    }

    private fun computeAdditions(purchaseId: UUID, linesToAdd: List<PurchaseLineCreateDto>): List<PurchaseLineEntity> {
        PurchaseValidator.guardPositiveLineQuantities(linesToAdd)
        locationProductService.guardAllActive(linesToAdd.map { it.locationProductId })
        val storageUnitsPerUnitByProductId = locationProductDataFetcher.getStorageUnitsPerUnit(
            linesToAdd.map { LocationProductUnitRequestDto(it.locationProductId, it.unitId) }
        )
        return linesToAdd.map {
            PurchaseMapper.toNewLineEntity(purchaseId, it, storageUnitsPerUnitByProductId.getValue(it.locationProductId))
        }
    }

    private data class UpdateResult(
        val toDelete: List<PurchaseLineEntity>,
        val toUpdate: List<PurchaseLineEntity>
    )

    private fun computeUpdates(
        linesToUpdate: List<PurchaseLineUpdateDto>,
        existingLinesById: Map<UUID, PurchaseLineEntity>
    ): UpdateResult {
        val toDelete = mutableListOf<PurchaseLineEntity>()
        val toSave = mutableListOf<PurchaseLineEntity>()

        val changedUnitLineDtos = linesToUpdate.filter { lineDto ->
            existingLinesById[lineDto.id]?.unitId != lineDto.unitId
        }
        val storageUnitsPerUnitForChangedProducts = locationProductDataFetcher.getStorageUnitsPerUnit(
            changedUnitLineDtos.map {
                LocationProductUnitRequestDto(existingLinesById[it.id]!!.locationProductId, it.unitId)
            }
        )

        for ((id, userProvidedQuantityOrdered, unitId, unitCost) in linesToUpdate) {
            val existing = existingLinesById[id] ?: continue
            PurchaseValidator.guardNonNegativeUpdateQuantity(userProvidedQuantityOrdered)
            if (userProvidedQuantityOrdered.compareTo(BigDecimal.ZERO) == 0) {
                toDelete.add(existing)
            } else {
                existing.userProvidedQuantityOrdered = userProvidedQuantityOrdered
                existing.unitCost = unitCost
                if (existing.unitId != unitId) {
                    existing.unitId = unitId
                    existing.storageUnitsPerUnit = storageUnitsPerUnitForChangedProducts.getValue(existing.locationProductId)
                }
                toSave.add(existing)
            }
        }

        return UpdateResult(toDelete, toSave)
    }
}
