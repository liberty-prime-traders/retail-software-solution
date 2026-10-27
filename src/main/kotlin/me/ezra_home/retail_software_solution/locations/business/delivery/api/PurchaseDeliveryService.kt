package me.ezra_home.retail_software_solution.locations.business.delivery.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.delivery.DeliveryHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.delivery.DeliveryRecord
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryLineRepository
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryMapper
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryRepository
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryValidator
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductUnitRequestDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.DeliveryHandlerForPurchase
import me.ezra_home.retail_software_solution.locations.business.purchase.api.DeliveryLineQuantity
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseResponseDto
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.api.PurchasePaymentStatusService
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class PurchaseDeliveryService(
  private val deliveryHandlerForPurchase: DeliveryHandlerForPurchase,
  private val deliveryRepository: PurchaseDeliveryRepository,
  private val deliveryLineRepository: PurchaseDeliveryLineRepository,
  private val deliveryHandlerForKafka: DeliveryHandlerForKafka,
  private val purchaseDeliveryValidator: PurchaseDeliveryValidator,
  private val locationProductDataFetcher: LocationProductDataFetcher,
  private val purchasePaymentStatusService: PurchasePaymentStatusService,
  private val fiscalPeriodService: FiscalPeriodService
) {

  fun recordDelivery(purchaseDeliveryCreateDto: PurchaseDeliveryCreateDto): PurchaseResponseDto {
    val deliveredDate = DateTimes.Local.atOrganizationZone(purchaseDeliveryCreateDto.deliveredAt)
    DateTimes.Local.requireNotInFuture(deliveredDate, "Delivery date")
    fiscalPeriodService.requireOpenForDate(deliveredDate)
    val context = deliveryHandlerForPurchase.prepareForDelivery(purchaseDeliveryCreateDto.purchaseId)
    purchaseDeliveryValidator.validate(purchaseDeliveryCreateDto, context.purchaseLineById)
    val storageUnitsPerUserProvidedUnitByPurchaseLineId = captureStorageUnitsPerUserProvidedUnit(
      purchaseDeliveryCreateDto, context.purchaseLineById
    )
    purchaseDeliveryValidator.guardWithinRemaining(
      purchaseDeliveryCreateDto,
      context.purchaseLineById,
      storageUnitsPerUserProvidedUnitByPurchaseLineId
    )

    val deliveryLines = purchaseDeliveryCreateDto.lines.map { line ->
      DeliveryLineQuantity(
        line.purchaseLineId,
        ExactQuantity.ofUserProvided(
          line.userProvidedQuantityDelivered,
          storageUnitsPerUserProvidedUnitByPurchaseLineId.getValue(line.purchaseLineId)
        )
      )
    }

    val deliveryRecord = persistDelivery(purchaseDeliveryCreateDto, storageUnitsPerUserProvidedUnitByPurchaseLineId)
    val response = deliveryHandlerForPurchase.commitDelivery(purchaseDeliveryCreateDto.purchaseId, deliveryLines)
    deliveryHandlerForKafka.publish(context, deliveryRecord)
    return response.copy(paymentStatus = purchasePaymentStatusService.patchThenReturnPaymentStatus(purchaseDeliveryCreateDto.purchaseId))
  }

  private fun captureStorageUnitsPerUserProvidedUnit(
    purchaseDeliveryCreateDto: PurchaseDeliveryCreateDto,
    purchaseLineById: Map<UUID, PurchaseLineDto>
  ): Map<UUID, Long> {
    val storageUnitsPerUserProvidedUnitByLocationProductId = locationProductDataFetcher.getStorageUnitsPerUserProvidedUnit(
      purchaseDeliveryCreateDto.lines.map {
        LocationProductUnitRequestDto(purchaseLineById.getValue(it.purchaseLineId).locationProductId, it.unitId)
      }
    )
    return purchaseDeliveryCreateDto.lines.associate {
      val locationProductId = purchaseLineById.getValue(it.purchaseLineId).locationProductId
      it.purchaseLineId to storageUnitsPerUserProvidedUnitByLocationProductId.getValue(locationProductId)
    }
  }

  private fun persistDelivery(
    purchaseDeliveryCreateDto: PurchaseDeliveryCreateDto,
    storageUnitsPerUserProvidedUnitByPurchaseLineId: Map<UUID, Long>
  ): DeliveryRecord {
    val delivery = deliveryRepository.save(PurchaseDeliveryMapper.toEntity(purchaseDeliveryCreateDto))
    val lines = PurchaseDeliveryMapper.toLineEntities(
      delivery.id!!,
      purchaseDeliveryCreateDto,
      storageUnitsPerUserProvidedUnitByPurchaseLineId
    )
    deliveryLineRepository.saveAll(lines)
    return DeliveryRecord(delivery, lines)
  }
}
