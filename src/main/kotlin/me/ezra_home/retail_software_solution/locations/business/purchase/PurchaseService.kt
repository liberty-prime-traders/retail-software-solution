package me.ezra_home.retail_software_solution.locations.business.purchase

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.purchase.dto.PurchaseCreateDto
import me.ezra_home.retail_software_solution.locations.business.purchase.dto.PurchaseLineCancelDto
import me.ezra_home.retail_software_solution.locations.business.purchase.dto.PurchaseResponseDto
import me.ezra_home.retail_software_solution.locations.business.purchase.dto.PurchaseUpdateDto
import me.ezra_home.retail_software_solution.locations.model.PurchaseLineEntity
import me.ezra_home.retail_software_solution.util.enums.PurchaseStatus
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class PurchaseService(
  private val purchaseRepository: PurchaseRepository,
  private val purchaseLineRepository: PurchaseLineRepository,
  private val purchaseAssembler: PurchaseAssembler
) {

  fun createDraft(dto: PurchaseCreateDto): PurchaseResponseDto {
    val purchase = PurchaseMapper.toDraftEntity(dto).also { purchaseRepository.save(it) }
    val lines = PurchaseMapper.toLineEntities(purchase.id!!, dto.lines)
    purchaseLineRepository.saveAll(lines)
    return purchaseAssembler.buildResponse(purchase, lines)
  }

  fun updateDraft(dto: PurchaseUpdateDto): PurchaseResponseDto {
    val purchase = purchaseRepository.findById(dto.id).orElseThrow { UpdatingNonExistingRecordException() }
    if (purchase.status != PurchaseStatus.DRAFT)
      throw RtsGenericException("Purchase is not in draft status anymore. The requested updates cannot be applied.")
    PurchaseMapper.applyDraftUpdate(purchase, dto)
    purchaseRepository.save(purchase)
    return purchaseAssembler.buildResponse(purchase, applyLineUpdates(purchase.id!!, dto))
  }

  fun createOrder(dto: PurchaseCreateDto): PurchaseResponseDto {
    if (dto.lines.isEmpty()) throw RtsGenericException("An order must have at least one item.")
    val purchase = PurchaseMapper.toOrderEntity(dto).also { purchaseRepository.save(it) }
    val lines = PurchaseMapper.toLineEntities(purchase.id!!, dto.lines)
    purchaseLineRepository.saveAll(lines)
    return purchaseAssembler.buildResponse(purchase, lines)
  }

  fun convertDraftToOrder(dto: PurchaseUpdateDto): PurchaseResponseDto {
    val purchase = purchaseRepository.findById(dto.id).orElseThrow { UpdatingNonExistingRecordException() }
    if (purchase.status != PurchaseStatus.DRAFT)
      throw RtsGenericException("Purchase is not in draft status anymore. The requested updates cannot be applied.")
    PurchaseMapper.convertDraftToOrder(purchase, dto)
    purchaseRepository.save(purchase)
    val lines = applyLineUpdates(purchase.id!!, dto)
    if (lines.isEmpty()) throw RtsGenericException("An order must have at least one item.")
    return purchaseAssembler.buildResponse(purchase, lines)
  }

  fun updateCancelQuantities(id: UUID, lines: List<PurchaseLineCancelDto>): PurchaseResponseDto {
    val purchase = purchaseRepository.findById(id).orElseThrow { UpdatingNonExistingRecordException() }
    if (purchase.status !in listOf(PurchaseStatus.ORDERED, PurchaseStatus.PARTIALLY_DELIVERED)) {
      throw RtsGenericException("Cannot cancel lines on a purchase with status ${purchase.status}")
    }
    val existingLines = purchaseLineRepository.findByPurchaseIdIn(listOf(id))
    val linesByProduct = existingLines.associateBy { it.locationProductId }
    val toSave = lines.mapNotNull { cancel ->
      linesByProduct[cancel.locationProductId]?.also { it.quantityCanceled = cancel.quantityCanceled }
    }
    purchaseLineRepository.saveAll(toSave)
    return purchaseAssembler.buildResponse(purchase, existingLines)
  }

  fun updateNotes(id: UUID, notes: String?) {
    val purchase = purchaseRepository.findById(id).orElseThrow { UpdatingNonExistingRecordException() }
    purchase.notes = notes
    purchaseRepository.save(purchase)
  }

  private fun applyLineUpdates(purchaseId: UUID, dto: PurchaseUpdateDto): List<PurchaseLineEntity> {
    val existingLines = purchaseLineRepository.findByPurchaseIdIn(listOf(purchaseId))
    val linesByProduct = existingLines.associateBy { it.locationProductId }
    val toDelete = mutableListOf<PurchaseLineEntity>()
    val toSave = mutableListOf<PurchaseLineEntity>()

    for (lineDto in dto.lines) {
      val existing = linesByProduct[lineDto.locationProductId]
      if (lineDto.quantityOrdered.compareTo(BigDecimal.ZERO) == 0) {
        existing?.let { toDelete.add(it) }
      } else {
        if (existing != null) {
          existing.quantityOrdered = lineDto.quantityOrdered
          existing.unitCost = lineDto.unitCost
          toSave.add(existing)
        } else {
          toSave.add(
            PurchaseLineEntity(
              purchaseId = purchaseId,
              locationProductId = lineDto.locationProductId,
              quantityOrdered = lineDto.quantityOrdered,
              unitCost = lineDto.unitCost
            )
          )
        }
      }
    }

    purchaseLineRepository.deleteAll(toDelete)
    purchaseLineRepository.saveAll(toSave)
    val deletedIds = toDelete.mapTo(HashSet()) { it.id }
    val savedIds = toSave.mapTo(HashSet()) { it.id }
    return existingLines.filter { it.id !in deletedIds && it.id !in savedIds } + toSave
  }
}
