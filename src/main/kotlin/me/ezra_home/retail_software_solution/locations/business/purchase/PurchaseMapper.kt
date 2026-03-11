package me.ezra_home.retail_software_solution.locations.business.purchase

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.purchase.dto.PurchaseCreateDto
import me.ezra_home.retail_software_solution.locations.business.purchase.dto.PurchaseLineCreateDto
import me.ezra_home.retail_software_solution.locations.business.purchase.dto.PurchaseLineUpdateDto
import me.ezra_home.retail_software_solution.locations.business.purchase.dto.PurchaseUpdateDto
import me.ezra_home.retail_software_solution.locations.model.PurchaseEntity
import me.ezra_home.retail_software_solution.locations.model.PurchaseLineEntity
import me.ezra_home.retail_software_solution.util.enums.PurchaseStatus
import java.time.OffsetDateTime
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
    status = PurchaseStatus.ORDERED,
    dateOrdered = dto.dateOrdered ?: OffsetDateTime.now(),
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
    purchase.dateOrdered = dto.dateOrdered?.orElseGet { OffsetDateTime.now() } ?: OffsetDateTime.now()
    purchase.orderedById = dto.orderedById?.orElseGet { SessionContextProvider.getUserId() } ?: SessionContextProvider.getUserId()
    purchase.status = PurchaseStatus.ORDERED
  }

  fun toNewLineEntity(purchaseId: UUID, dto: PurchaseLineUpdateDto) = PurchaseLineEntity(
    purchaseId = purchaseId,
    locationProductId = dto.locationProductId!!,
    quantityOrdered = dto.quantityOrdered,
    unitCost = dto.unitCost
  )

  fun toLineEntities(purchaseId: UUID, lines: List<PurchaseLineCreateDto>) = lines.map {
    PurchaseLineEntity(
      purchaseId = purchaseId,
      locationProductId = it.locationProductId,
      quantityOrdered = it.quantityOrdered,
      unitCost = it.unitCost
    )
  }
}
