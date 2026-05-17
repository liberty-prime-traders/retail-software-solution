package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleCommitInput
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleCommitLine
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.util.UUID

object SaleCommitLineSync {

    data class Result(
        val lineIdByClientKey: Map<UUID, UUID>,
        val persistedLines: List<SaleLineEntity>,
    )

    fun sync(
        sale: SaleEntity,
        input: SaleCommitInput,
        saleLineRepository: SaleLineRepository,
    ): Result {
        val saleId = sale.id!!
        val existing = saleLineRepository.findBySaleId(saleId).associateBy { it.id!! }
        val incomingIds = input.lines.mapNotNull { it.existingId }.toHashSet()
        val toDelete = existing.keys - incomingIds
        if (toDelete.isNotEmpty()) {
            saleLineRepository.deleteAllById(toDelete)
        }

        val persisted = mutableListOf<SaleLineEntity>()
        val idsByClientKey = mutableMapOf<UUID, UUID>()
        input.lines.forEach { lineInput ->
            val entity = if (lineInput.existingId != null) {
                existing[lineInput.existingId]?.also {
                    it.locationProductId = lineInput.locationProductId
                    it.quantity = lineInput.quantity
                    it.unitId = lineInput.unitId
                    it.unitPrice = lineInput.unitPrice
                    it.conversionFactor = lineInput.conversionFactor
                } ?: throw RtsGenericException("Sale line ${lineInput.existingId} no longer exists")
            } else {
                lineInput.toEntity(saleId)
            }
            persisted.add(entity)
        }
        val saved = saleLineRepository.saveAll(persisted)
        saved.forEachIndexed { index, savedLine ->
            idsByClientKey[input.lines[index].clientKey] = savedLine.id!!
        }
        return Result(idsByClientKey, saved.toList())
    }

    private fun SaleCommitLine.toEntity(saleId: UUID) = SaleLineEntity(
        saleId = saleId,
        locationProductId = locationProductId,
        quantity = quantity,
        unitId = unitId,
        unitPrice = unitPrice,
        conversionFactor = conversionFactor,
    )
}
