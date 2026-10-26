package me.ezra_home.retail_software_solution.locations.business.purchase

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseCreateDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineCreateDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseStatus
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseUpdateDto
import me.ezra_home.retail_software_solution.util.business.DateTimes
import java.util.UUID

object PurchaseMapper {

  fun toDraftEntity(purchaseCreateDto: PurchaseCreateDto) = PurchaseEntity(
    supplierId = purchaseCreateDto.supplierId,
    notes = purchaseCreateDto.notes,
    dateOrdered = purchaseCreateDto.dateOrdered,
    orderedById = purchaseCreateDto.orderedById
  )

  fun toOrderEntity(purchaseCreateDto: PurchaseCreateDto) = PurchaseEntity(
    supplierId = purchaseCreateDto.supplierId,
    notes = purchaseCreateDto.notes,
    purchaseStatus = PurchaseStatus.ORDERED,
    dateOrdered = purchaseCreateDto.dateOrdered ?: DateTimes.Offset.Now.organization(),
    orderedById = purchaseCreateDto.orderedById ?: SessionContextProvider.getUserId()
  )

  fun applyDraftUpdate(purchase: PurchaseEntity, purchaseUpdateDto: PurchaseUpdateDto) {
    purchaseUpdateDto.supplierId?.let { purchase.supplierId = it }
    purchaseUpdateDto.notes?.let { purchase.notes = it.orElse(null) }
    purchaseUpdateDto.dateOrdered?.let { purchase.dateOrdered = it.orElse(null) }
    purchaseUpdateDto.orderedById?.let { purchase.orderedById = it.orElse(null) }
  }

  fun convertDraftToOrder(purchase: PurchaseEntity, purchaseUpdateDto: PurchaseUpdateDto) {
    purchaseUpdateDto.supplierId?.let { purchase.supplierId = it }
    purchaseUpdateDto.notes?.let { purchase.notes = it.orElse(null) }
    purchase.dateOrdered = purchaseUpdateDto.dateOrdered?.orElse(null) ?: DateTimes.Offset.Now.organization()
    purchase.orderedById = purchaseUpdateDto.orderedById?.orElse(null) ?: SessionContextProvider.getUserId()
    purchase.purchaseStatus = PurchaseStatus.ORDERED
  }

  fun toNewLineEntity(purchaseId: UUID, purchaseLineCreateDto: PurchaseLineCreateDto, storageUnitsPerUnit: Long) = PurchaseLineEntity(
    purchaseId = purchaseId,
    locationProductId = purchaseLineCreateDto.locationProductId,
    userProvidedQuantityOrdered = purchaseLineCreateDto.userProvidedQuantityOrdered.toBigDecimal(),
    unitCost = purchaseLineCreateDto.unitCost.toBigDecimal(),
    unitId = purchaseLineCreateDto.unitId,
    storageUnitsPerUnit = storageUnitsPerUnit
  )

  fun toLineEntities(purchaseId: UUID, lines: List<PurchaseLineCreateDto>, storageUnitsPerUnitByProductId: Map<UUID, Long>) =
    lines.map { toNewLineEntity(purchaseId, it, storageUnitsPerUnitByProductId.getValue(it.locationProductId)) }

  fun purchaseLineEntityToDto(purchaseLineEntity: PurchaseLineEntity) = PurchaseLineDto(
    id = purchaseLineEntity.id!!,
    purchaseId = purchaseLineEntity.purchaseId,
    locationProductId = purchaseLineEntity.locationProductId,
    unitCost = purchaseLineEntity.unitCost,
    unitId = purchaseLineEntity.unitId,
    storageUnitsPerUnit = purchaseLineEntity.storageUnitsPerUnit,
    storageQuantityExpected = purchaseLineEntity.storageQuantityExpected(),
    storageQuantityRemaining = purchaseLineEntity.storageQuantityRemaining()
  )
}
