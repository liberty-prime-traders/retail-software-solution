package me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.StockTransferOrderEntity
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.StockTransferOrderRepository
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class StockTransferOrderService(
    private val stockTransferOrderRepository: StockTransferOrderRepository
) {

    fun createOrder(
        sourceLocationId: UUID,
        destinationLocationId: UUID,
        notes: String?
    ): StockTransferOrderDomainDto {
        val entity = StockTransferOrderEntity(
            sourceLocationId = sourceLocationId,
            destinationLocationId = destinationLocationId,
            notes = notes
        )
        return stockTransferOrderRepository.save(entity).toDomainDto()
    }

    fun updateStatusToCompleted(referenceNumber: String): StockTransferOrderDomainDto =
        updateStatus(referenceNumber, StockTransferStatus.COMPLETED)

    private fun updateStatus(referenceNumber: String, newStatus: StockTransferStatus): StockTransferOrderDomainDto {
        val entity = findByReferenceNumberOrThrow(referenceNumber)
        entity.status = newStatus
        return stockTransferOrderRepository.save(entity).toDomainDto()
    }

    fun updateStatusToDispatched(referenceNumber: String): StockTransferOrderDomainDto =
        updateStatus(referenceNumber, StockTransferStatus.DISPATCHED)

    fun updateStatusToCancelled(referenceNumber: String): StockTransferOrderDomainDto =
        updateStatus(referenceNumber, StockTransferStatus.CANCELLED)

    fun setDispatchSummary(
        referenceNumber: String,
        lineCount: Int,
        totalDispatchedCost: BigDecimal,
        dispatchedAt: OffsetDateTime,
        dispatchedByName: String?
    ) {
        val entity = findByReferenceNumberOrThrow(referenceNumber)
        entity.lineCount = lineCount
        entity.totalDispatchedCost = totalDispatchedCost
        entity.dispatchedAt = dispatchedAt
        entity.dispatchedByName = dispatchedByName
        stockTransferOrderRepository.save(entity)
    }

    private fun findByReferenceNumberOrThrow(referenceNumber: String): StockTransferOrderEntity =
        stockTransferOrderRepository.findByReferenceNumber(referenceNumber)
            ?: throw RtsGenericException("Stock transfer order $referenceNumber not found")

    private fun StockTransferOrderEntity.toDomainDto() = StockTransferOrderDomainDto(
        id = id!!,
        referenceNumber = requiredReference(),
        sourceLocationId = sourceLocationId,
        destinationLocationId = destinationLocationId,
        status = status,
        notes = notes,
        lineCount = lineCount,
        totalDispatchedCost = totalDispatchedCost,
        dispatchedAt = dispatchedAt,
        dispatchedByName = dispatchedByName,
        createdById = createdById!!,
        createdOn = createdOn!!
    )
}
