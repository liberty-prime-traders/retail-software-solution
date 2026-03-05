package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.delivery.dto.PurchaseDeliveryCreateDto
import me.ezra_home.retail_software_solution.locations.business.delivery.dto.PurchaseDeliveryResponseDto
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseLineRepository
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseRepository
import me.ezra_home.retail_software_solution.util.enums.PurchaseStatus
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
@TransactionalOnLocationSchema
class PurchaseDeliveryService(
  private val purchaseRepository: PurchaseRepository,
  private val purchaseLineRepository: PurchaseLineRepository,
  private val deliveryRepository: PurchaseDeliveryRepository,
  private val deliveryLineRepository: PurchaseDeliveryLineRepository,
  private val assembler: PurchaseDeliveryAssembler,
  private val eventPublisher: ApplicationEventPublisher
) {

  fun recordDelivery(dto: PurchaseDeliveryCreateDto): PurchaseDeliveryResponseDto {
    val purchase = purchaseRepository.findById(dto.purchaseId).orElseThrow { UpdatingNonExistingRecordException() }
    if (purchase.status == PurchaseStatus.CANCELED)
      throw RtsGenericException("Cannot record a delivery on a canceled purchase.")
    if (purchase.status == PurchaseStatus.FULLY_DELIVERED)
      throw RtsGenericException("Cannot record a delivery on a fully delivered purchase.")

    val purchaseLines = purchaseLineRepository.findByPurchaseId(dto.purchaseId)
    val purchaseLineById = purchaseLines.associateBy { it.id!! }
    PurchaseDeliveryValidator.validate(dto, purchaseLineById)

    val modifiedLines = dto.lines.map { lineDto ->
      purchaseLineById[lineDto.purchaseLineId]!!.also {
        it.quantityDelivered = it.quantityDelivered.add(lineDto.quantityDelivered)
      }
    }

    val delivery = deliveryRepository.save(PurchaseDeliveryMapper.toEntity(dto))
    val deliveryLines = PurchaseDeliveryMapper.toLineEntities(delivery.id!!, dto)
    purchaseLineRepository.saveAll(modifiedLines)
    deliveryLineRepository.saveAll(deliveryLines)

    val fullyDelivered = purchaseLines.all {
      it.quantityOrdered - it.quantityCanceled - it.quantityDelivered <= BigDecimal.ZERO
    }
    purchase.status = if (fullyDelivered) PurchaseStatus.FULLY_DELIVERED else PurchaseStatus.PARTIALLY_DELIVERED
    purchaseRepository.save(purchase)

    val sourceSchema = SessionContextProvider.getSession().locationSchemaName
      ?: throw RtsGenericException("Location schema not found in session.")
    eventPublisher.publishEvent(
      PurchaseDeliveryMapper.toEvent(purchase, delivery, deliveryLines, purchaseLineById, sourceSchema)
    )

    return assembler.buildResponse(delivery, deliveryLines, purchaseLineById)
  }
}
