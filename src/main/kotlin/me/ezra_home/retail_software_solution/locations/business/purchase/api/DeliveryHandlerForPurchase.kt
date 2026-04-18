package me.ezra_home.retail_software_solution.locations.business.purchase.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseAssembler
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseEntity
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseLineEntity
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseLineRepository
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseMapper
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseRepository
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class DeliveryHandlerForPurchase(
    private val purchaseRepository: PurchaseRepository,
    private val purchaseLineRepository: PurchaseLineRepository,
    private val purchaseAssembler: PurchaseAssembler
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun prepareForDelivery(purchaseId: UUID): PurchaseDeliveryContext {
        val purchase = purchaseRepository.getReferenceById(purchaseId)
        if (purchase.purchaseStatus == PurchaseStatus.CANCELED)
            throw RtsGenericException("Cannot record a delivery on a canceled purchase.")
        if (purchase.purchaseStatus == PurchaseStatus.FULLY_DELIVERED)
            throw RtsGenericException("Cannot record a delivery on a fully delivered purchase.")
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
    fun commitDelivery(purchaseId: UUID, deliveries: List<Pair<UUID, BigDecimal>>): PurchaseResponseDto {
        val purchase = purchaseRepository.getReferenceById(purchaseId)
        val purchaseLines = purchaseLineRepository.findByPurchaseId(purchaseId)
        val lineById = purchaseLines.associateBy { it.id!! }
        val toSave = deliveries.map { (lineId, qty) ->
            lineById[lineId]?.also { it.quantityDelivered = it.quantityDelivered.add(qty) }
                ?: throw RtsGenericException("Purchase line $lineId not found")
        }
        purchaseLineRepository.saveAll(toSave)
        purchase.purchaseStatus = resolveDeliveryStatus(purchaseLines)
        purchaseRepository.save(purchase)
        return purchaseAssembler.buildResponse(purchase, purchaseLines)
    }

    private fun resolveDeliveryStatus(purchaseLines: List<PurchaseLineEntity>): PurchaseStatus {
        val fullyDelivered = purchaseLines.all {
            it.getExpectedQuantity() - it.quantityDelivered <= BigDecimal.ZERO
        }
        return if (fullyDelivered) PurchaseStatus.FULLY_DELIVERED else PurchaseStatus.PARTIALLY_DELIVERED
    }
}
