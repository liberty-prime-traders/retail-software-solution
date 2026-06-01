package me.ezra_home.retail_software_solution.locations.business.purchase

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseCreateDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineCreateDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseStatus
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseUpdateDto
import me.ezra_home.retail_software_solution.util.business.DateTimes
import java.math.BigDecimal
import java.util.UUID

object PurchaseMapper {

  fun toDraftEntity(dto: PurchaseCreateDto) = PurchaseEntity(
    supplierId = dto.supplierId,
    notes = dto.notes,
    dateOrdered = dto.dateOrdered,
    orderedById = dto.orderedById
  )

  fun toOrderEntity(dto: PurchaseCreateDto) = PurchaseEntity(
    supplierId = dto.supplierId,
    notes = dto.notes,
    purchaseStatus = PurchaseStatus.ORDERED,
    dateOrdered = dto.dateOrdered ?: DateTimes.Offset.Now.organization(),
    orderedById = dto.orderedById ?: SessionContextProvider.getUserId()
  )

  fun applyDraftUpdate(purchase: PurchaseEntity, dto: PurchaseUpdateDto) {
    dto.supplierId?.let { purchase.supplierId = it }
    dto.notes?.let { purchase.notes = it.orElse(null) }
    dto.dateOrdered?.let { purchase.dateOrdered = it.orElse(null) }
    dto.orderedById?.let { purchase.orderedById = it.orElse(null) }
  }

  fun convertDraftToOrder(purchase: PurchaseEntity, dto: PurchaseUpdateDto) {
    dto.supplierId?.let { purchase.supplierId = it }
    dto.notes?.let { purchase.notes = it.orElse(null) }
    purchase.dateOrdered = dto.dateOrdered?.orElse(null) ?: DateTimes.Offset.Now.organization()
    purchase.orderedById = dto.orderedById?.orElse(null) ?: SessionContextProvider.getUserId()
    purchase.purchaseStatus = PurchaseStatus.ORDERED
  }

  fun toNewLineEntity(purchaseId: UUID, dto: PurchaseLineCreateDto, conversionFactor: BigDecimal) = PurchaseLineEntity(
    purchaseId = purchaseId,
    locationProductId = dto.locationProductId,
    quantityOrdered = dto.quantityOrdered,
    unitCost = dto.unitCost,
    unitId = dto.unitId,
    conversionFactor = conversionFactor
  )

  fun toLineEntities(purchaseId: UUID, lines: List<PurchaseLineCreateDto>, factorByProductId: Map<UUID, BigDecimal>) = lines.map {
    PurchaseLineEntity(
      purchaseId = purchaseId,
      locationProductId = it.locationProductId,
      quantityOrdered = it.quantityOrdered,
      unitCost = it.unitCost,
      unitId = it.unitId,
      conversionFactor = factorByProductId.getValue(it.locationProductId)
    )
  }

  fun purchaseLineEntityToDto(entity: PurchaseLineEntity) = PurchaseLineDto(
    id = entity.id!!,
    purchaseId = entity.purchaseId,
    locationProductId = entity.locationProductId,
    unitCost = entity.unitCost,
    unitId = entity.unitId,
    conversionFactor = entity.conversionFactor,
    expectedQuantity = entity.getExpectedQuantity(),
    remainingQuantity = entity.getRemainingQuantity()
  )
}
