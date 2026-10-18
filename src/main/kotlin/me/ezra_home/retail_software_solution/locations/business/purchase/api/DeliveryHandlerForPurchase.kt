package me.ezra_home.retail_software_solution.locations.business.purchase.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseAssembler
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseEntity
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseLineEntity
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseLineRepository
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseMapper
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseRepository
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseValidator
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

data class DeliveryLineQuantity(val purchaseLineId: UUID, val storageQuantity: ExactQuantity)

@Service
class DeliveryHandlerForPurchase(
    private val purchaseRepository: PurchaseRepository,
    private val purchaseLineRepository: PurchaseLineRepository,
    private val purchaseAssembler: PurchaseAssembler,
    private val purchaseDataFetcher: PurchaseDataFetcher,
) {

    @TransactionalOnLocationSchema
    fun prepareForDelivery(purchaseId: UUID): PurchaseDeliveryContext {
        val purchase = purchaseDataFetcher.lockAndGetPurchase(purchaseId)
        PurchaseValidator.guardCanReceiveDelivery(purchase)
        return buildContext(purchase)
    }

    @TransactionalOnLocationSchema(readOnly = true)
    fun getDeliveryContext(purchaseId: UUID): PurchaseDeliveryContext {
        return buildContext(purchaseRepository.getReferenceById(purchaseId))
    }

    private fun buildContext(purchase: PurchaseEntity): PurchaseDeliveryContext {
        val lines = purchaseLineRepository.findByPurchaseId(purchase.id!!)
        return PurchaseDeliveryContext(
            purchaseId = purchase.id!!,
            supplierId = purchase.supplierId,
            purchaseLineById = lines.associateBy { it.id!! }.mapValues {
                PurchaseMapper.purchaseLineEntityToDto(it.value)
            }
        )
    }
    
    @TransactionalOnLocationSchema
    fun commitDelivery(purchaseId: UUID, deliveryLines: List<DeliveryLineQuantity>): PurchaseResponseDto {
        val purchase = purchaseRepository.getReferenceById(purchaseId)
        val purchaseLines = purchaseLineRepository.findByPurchaseId(purchaseId)
        val lineById = purchaseLines.associateBy { it.id!! }
        val toSave = deliveryLines.map { (lineId, storageQuantity) ->
            lineById[lineId]?.also {
                it.storageQuantityDelivered = (ExactQuantity.ofStorage(it.storageQuantityDelivered) + storageQuantity).toStorageQuantity()
            } ?: throw RtsGenericException("Purchase line $lineId not found")
        }
        purchaseLineRepository.saveAll(toSave)
        purchase.purchaseStatus = resolveDeliveryStatus(purchaseLines)
        purchaseRepository.save(purchase)
        return purchaseAssembler.buildResponse(purchase, purchaseLines)
    }

    private fun resolveDeliveryStatus(purchaseLines: List<PurchaseLineEntity>): PurchaseStatus {
        val fullyDelivered = purchaseLines.none { it.storageQuantityRemaining().isPositive() }
        return if (fullyDelivered) PurchaseStatus.FULLY_DELIVERED else PurchaseStatus.PARTIALLY_DELIVERED
    }
}
