package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleSaveRequest
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineSaveRequest
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.PersistedSaleLine
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.util.UUID

object SaleLineSync {

    data class Result(
        val saleLineIdsByClientKey: Map<UUID, UUID>,
        val persistedSaleLines: List<SaleLineEntity>,
    ) {
        fun toPersistedSaleLines(): List<PersistedSaleLine> = persistedSaleLines.map { saleLineEntity ->
            PersistedSaleLine(
                id = saleLineEntity.id!!,
                locationProductId = saleLineEntity.locationProductId,
                quantity = saleLineEntity.quantity,
                unitPrice = saleLineEntity.unitPrice,
            )
        }
    }

    fun sync(saleEntity: SaleEntity, saleSaveRequest: SaleSaveRequest, saleLineRepository: SaleLineRepository): Result {
        val saleId = saleEntity.id!!
        val existingSaleLinesById = saleLineRepository.findBySaleId(saleId).associateBy { it.id!! }
        val incomingSaleLineIds = saleSaveRequest.saleLines.mapNotNull { it.existingId }.toHashSet()
        val saleLineIdsToDelete = existingSaleLinesById.keys - incomingSaleLineIds
        if (saleLineIdsToDelete.isNotEmpty()) {
            saleLineRepository.deleteAllById(saleLineIdsToDelete)
        }

        val saleLinesToPersist = mutableListOf<SaleLineEntity>()
        val saleLineIdsByClientKey = mutableMapOf<UUID, UUID>()
        saleSaveRequest.saleLines.forEach { saleLineSaveRequest ->
            val saleLineEntity = if (saleLineSaveRequest.existingId != null) {
                existingSaleLinesById[saleLineSaveRequest.existingId]?.also { existingSaleLineEntity ->
                    existingSaleLineEntity.locationProductId = saleLineSaveRequest.locationProductId
                    existingSaleLineEntity.quantity = saleLineSaveRequest.quantity
                    existingSaleLineEntity.unitId = saleLineSaveRequest.unitId
                    existingSaleLineEntity.unitPrice = saleLineSaveRequest.unitPrice
                    existingSaleLineEntity.conversionFactor = saleLineSaveRequest.conversionFactor
                } ?: throw RtsGenericException("Sale line ${saleLineSaveRequest.existingId} no longer exists")
            } else {
                saleLineSaveRequest.toEntity(saleId)
            }
            saleLinesToPersist.add(saleLineEntity)
        }
        val savedSaleLines = saleLineRepository.saveAll(saleLinesToPersist)
        savedSaleLines.forEachIndexed { saleLineIndex, savedSaleLineEntity ->
            saleLineIdsByClientKey[saleSaveRequest.saleLines[saleLineIndex].clientKey] = savedSaleLineEntity.id!!
        }
        return Result(saleLineIdsByClientKey, savedSaleLines.toList())
    }

    private fun SaleLineSaveRequest.toEntity(saleId: UUID) = SaleLineEntity(
        saleId = saleId,
        locationProductId = locationProductId,
        quantity = quantity,
        unitId = unitId,
        unitPrice = unitPrice,
        conversionFactor = conversionFactor,
    )
}
