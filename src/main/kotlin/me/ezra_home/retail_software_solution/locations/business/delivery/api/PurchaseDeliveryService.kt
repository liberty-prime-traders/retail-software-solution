package me.ezra_home.retail_software_solution.locations.business.delivery.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.delivery.DeliveryHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.delivery.DeliveryRecord
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryLineRepository
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryMapper
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryRepository
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryValidator
import me.ezra_home.retail_software_solution.locations.business.purchase.api.DeliveryHandlerForPurchase
import me.ezra_home.retail_software_solution.locations.business.purchase.api.DeliveryLineQuantity
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseResponseDto
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphBuilder
import me.ezra_home.retail_software_solution.util.business.Decimals
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema
class PurchaseDeliveryService(
  private val deliveryHandlerForPurchase: DeliveryHandlerForPurchase,
  private val deliveryRepository: PurchaseDeliveryRepository,
  private val deliveryLineRepository: PurchaseDeliveryLineRepository,
  private val deliveryHandlerForKafka: DeliveryHandlerForKafka,
  private val purchaseDeliveryValidator: PurchaseDeliveryValidator,
  private val unitConversionGraphBuilder: UnitConversionGraphBuilder
) {

  fun recordDelivery(dto: PurchaseDeliveryCreateDto): PurchaseResponseDto {
    val context = deliveryHandlerForPurchase.prepareForDelivery(dto.purchaseId)
    purchaseDeliveryValidator.validate(dto, context.purchaseLineById)

    val deliveryRecord = persistDelivery(dto)
    deliveryHandlerForKafka.publish(context, deliveryRecord)

    val deliveries = dto.lines.map { line ->
      val purchaseLine = context.purchaseLineById[line.purchaseLineId]!!
      val factor = unitConversionGraphBuilder.getFactor(line.unitId, purchaseLine.unitId)
      DeliveryLineQuantity(line.purchaseLineId, Decimals.multiplyScale4(line.quantityDelivered, factor))
    }
    return deliveryHandlerForPurchase.commitDelivery(dto.purchaseId, deliveries)
  }

  private fun persistDelivery(dto: PurchaseDeliveryCreateDto): DeliveryRecord {
    val delivery = deliveryRepository.save(PurchaseDeliveryMapper.toEntity(dto))
    val lines = PurchaseDeliveryMapper.toLineEntities(delivery.id!!, dto)
    deliveryLineRepository.saveAll(lines)
    return DeliveryRecord(delivery, lines)
  }
}
