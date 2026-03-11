package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.delivery.dto.PurchaseDeliveryCreateDto
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseAssembler
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseLineRepository
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseRepository
import me.ezra_home.retail_software_solution.locations.business.purchase.dto.PurchaseResponseDto
import me.ezra_home.retail_software_solution.locations.model.PurchaseDeliveryEntity
import me.ezra_home.retail_software_solution.locations.model.PurchaseDeliveryLineEntity
import me.ezra_home.retail_software_solution.locations.model.PurchaseEntity
import me.ezra_home.retail_software_solution.locations.model.PurchaseLineEntity
import me.ezra_home.retail_software_solution.util.enums.PurchaseStatus
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class PurchaseDeliveryService(
  private val purchaseRepository: PurchaseRepository,
  private val purchaseLineRepository: PurchaseLineRepository,
  private val deliveryRepository: PurchaseDeliveryRepository,
  private val deliveryLineRepository: PurchaseDeliveryLineRepository,
  private val purchaseAssembler: PurchaseAssembler,
  private val eventPublisher: ApplicationEventPublisher
) {

  fun recordDelivery(dto: PurchaseDeliveryCreateDto): PurchaseResponseDto {
    val purchase = purchaseRepository.findById(dto.purchaseId).orElseThrow { UpdatingNonExistingRecordException() }
    guardDeliveryStatus(purchase)

    val purchaseLines = purchaseLineRepository.findByPurchaseId(dto.purchaseId)
    val purchaseLineById = purchaseLines.associateBy { it.id!! }
    PurchaseDeliveryValidator.validate(dto, purchaseLineById)

    persistDeliveredQuantities(dto, purchaseLineById)
    val (delivery, deliveryLines) = persistDelivery(dto)
    purchase.status = resolveDeliveryStatus(purchaseLines)
    purchaseRepository.save(purchase)

    publishDeliveryEvent(purchase, delivery, deliveryLines, purchaseLineById)

    return purchaseAssembler.buildResponse(purchase, purchaseLines)
  }

  private fun guardDeliveryStatus(purchase: PurchaseEntity) {
    if (purchase.status == PurchaseStatus.CANCELED)
      throw RtsGenericException("Cannot record a delivery on a canceled purchase.")
    if (purchase.status == PurchaseStatus.FULLY_DELIVERED)
      throw RtsGenericException("Cannot record a delivery on a fully delivered purchase.")
  }

  private fun persistDeliveredQuantities(dto: PurchaseDeliveryCreateDto, purchaseLineById: Map<UUID, PurchaseLineEntity>) {
    val toSave = dto.lines.map { lineDto ->
      purchaseLineById[lineDto.purchaseLineId]!!.also {
        it.quantityDelivered = it.quantityDelivered.add(lineDto.quantityDelivered)
      }
    }
    purchaseLineRepository.saveAll(toSave)
  }

  private fun persistDelivery(dto: PurchaseDeliveryCreateDto): Pair<PurchaseDeliveryEntity, List<PurchaseDeliveryLineEntity>> {
    val delivery = deliveryRepository.save(PurchaseDeliveryMapper.toEntity(dto))
    val deliveryLines = PurchaseDeliveryMapper.toLineEntities(delivery.id!!, dto)
    deliveryLineRepository.saveAll(deliveryLines)
    return delivery to deliveryLines
  }

  private fun resolveDeliveryStatus(purchaseLines: List<PurchaseLineEntity>): PurchaseStatus {
    val fullyDelivered = purchaseLines.all {
      it.quantityOrdered - it.quantityCanceled - it.quantityDelivered <= BigDecimal.ZERO
    }
    return if (fullyDelivered) PurchaseStatus.FULLY_DELIVERED else PurchaseStatus.PARTIALLY_DELIVERED
  }

  private fun publishDeliveryEvent(
    purchase: PurchaseEntity,
    delivery: PurchaseDeliveryEntity,
    deliveryLines: List<PurchaseDeliveryLineEntity>,
    purchaseLineById: Map<UUID, PurchaseLineEntity>
  ) {
    val sourceSchema = SessionContextProvider.getSession().locationSchemaName
      ?: throw RtsGenericException("Location schema not found in session.")
    eventPublisher.publishEvent(
      PurchaseDeliveryMapper.toEvent(purchase, delivery, deliveryLines, purchaseLineById, sourceSchema)
    )
  }
}
