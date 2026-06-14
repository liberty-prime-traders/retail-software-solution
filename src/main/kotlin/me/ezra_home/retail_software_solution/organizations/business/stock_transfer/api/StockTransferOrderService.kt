package me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.StockTransferOrderEntity
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.StockTransferOrderRepository
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class StockTransferOrderService(
    private val stockTransferOrderRepository: StockTransferOrderRepository
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getById(id: UUID): StockTransferOrderDomainDto =
        stockTransferOrderRepository.findById(id)
            .orElseThrow { RtsGenericException("Stock transfer order $id not found") }
            .toDomainDto()

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getByReferenceNumber(referenceNumber: String): StockTransferOrderDomainDto =
        findByReferenceNumberOrThrow(referenceNumber).toDomainDto()

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getTopByLocationId(locationId: UUID, limit: Int): List<StockTransferOrderDomainDto> =
        stockTransferOrderRepository
            .findTopNByLocation(locationId, org.springframework.data.domain.PageRequest.of(0, limit))
            .map { it.toDomainDto() }

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

    fun updateStatus(referenceNumber: String, newStatus: StockTransferStatus): StockTransferOrderDomainDto {
        val entity = findByReferenceNumberOrThrow(referenceNumber)
        entity.status = newStatus
        return stockTransferOrderRepository.save(entity).toDomainDto()
    }

    fun updateStatusToCompleted(referenceNumber: String): StockTransferOrderDomainDto =
        updateStatus(referenceNumber, StockTransferStatus.COMPLETED)

    fun updateStatusToDispatched(referenceNumber: String): StockTransferOrderDomainDto =
        updateStatus(referenceNumber, StockTransferStatus.DISPATCHED)

    fun updateStatusToCancelled(referenceNumber: String): StockTransferOrderDomainDto =
        updateStatus(referenceNumber, StockTransferStatus.CANCELLED)

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
        createdById = createdById!!,
        createdOn = createdOn!!
    )
}
