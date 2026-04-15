package me.ezra_home.retail_software_solution.locations.business.delivery.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.delivery.DeliveryRecord
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryLineRepository
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryMapper
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryRepository
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryValidator
import me.ezra_home.retail_software_solution.locations.business.purchase.api.DeliveryHandlerForPurchase
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDeliveryContext
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseResponseDto
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema
class PurchaseDeliveryService(
  private val deliveryHandlerForPurchase: DeliveryHandlerForPurchase,
  private val deliveryRepository: PurchaseDeliveryRepository,
  private val deliveryLineRepository: PurchaseDeliveryLineRepository,
  private val eventPublisher: ApplicationEventPublisher
) {

  fun recordDelivery(dto: PurchaseDeliveryCreateDto): PurchaseResponseDto {
    val context = deliveryHandlerForPurchase.prepareForDelivery(dto.purchaseId)
    PurchaseDeliveryValidator.validate(dto, context.purchaseLineById)

    val deliveryRecord = persistDelivery(dto)
    publishDeliveryEvent(context, deliveryRecord)

    val deliveries = dto.lines.map { it.purchaseLineId to it.quantityDelivered }
    return deliveryHandlerForPurchase.commitDelivery(dto.purchaseId, deliveries)
  }

  private fun persistDelivery(dto: PurchaseDeliveryCreateDto): DeliveryRecord {
    val delivery = deliveryRepository.save(PurchaseDeliveryMapper.toEntity(dto))
    val lines = PurchaseDeliveryMapper.toLineEntities(delivery.id!!, dto)
    deliveryLineRepository.saveAll(lines)
    return DeliveryRecord(delivery, lines)
  }

  private fun publishDeliveryEvent(context: PurchaseDeliveryContext, deliveryRecord: DeliveryRecord) {
    val sourceSchema = SessionContextProvider.getLocationSchema()
    eventPublisher.publishEvent(
      PurchaseDeliveryMapper.toEvent(context.purchaseId, context.supplierId, deliveryRecord, context.purchaseLineById, sourceSchema)
    )
  }
}
