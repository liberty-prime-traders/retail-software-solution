package me.ezra_home.retail_software_solution.locations.business.purchase.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseAssembler
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseLineEntity
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseLineRepository
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseMapper
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseRepository
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseValidator
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

private fun PurchaseLineEntity.toDto() = PurchaseLineDto(
    id = getNullSafeId(),
    purchaseId = purchaseId,
    locationProductId = locationProductId,
    quantityOrdered = quantityOrdered,
    unitCost = unitCost,
    quantityDelivered = quantityDelivered,
    quantityCanceled = quantityCanceled
)

@Service
@TransactionalOnLocationSchema
class PurchaseService(
  private val purchaseRepository: PurchaseRepository,
  private val purchaseLineRepository: PurchaseLineRepository,
  private val purchaseAssembler: PurchaseAssembler
) {

  fun prepareForDelivery(purchaseId: UUID): PurchaseDeliveryContext {
      val purchase = purchaseRepository.findById(purchaseId).orElseThrow { UpdatingNonExistingRecordException() }
      if (purchase.status == PurchaseStatus.CANCELED)
          throw RtsGenericException("Cannot record a delivery on a canceled purchase.")
      if (purchase.status == PurchaseStatus.FULLY_DELIVERED)
          throw RtsGenericException("Cannot record a delivery on a fully delivered purchase.")
      val lines = purchaseLineRepository.findByPurchaseId(purchaseId)
      return PurchaseDeliveryContext(
          purchaseId = purchase.getNullSafeId(),
          supplierId = purchase.supplierId,
          purchaseLineById = lines.associateBy { it.getNullSafeId() }.mapValues { it.value.toDto() }
      )
  }

  fun commitDelivery(purchaseId: UUID, deliveries: List<Pair<UUID, BigDecimal>>): PurchaseResponseDto {
      val purchase = purchaseRepository.findById(purchaseId).orElseThrow { UpdatingNonExistingRecordException() }
      val purchaseLines = purchaseLineRepository.findByPurchaseId(purchaseId)
      val lineById = purchaseLines.associateBy { it.getNullSafeId() }
      val toSave = deliveries.map { (lineId, qty) ->
          lineById[lineId]?.also { it.quantityDelivered = it.quantityDelivered.add(qty) }
              ?: throw RtsGenericException("Purchase line $lineId not found")
      }
      purchaseLineRepository.saveAll(toSave)
      purchase.status = resolveDeliveryStatus(purchaseLines)
      purchaseRepository.save(purchase)
      return purchaseAssembler.buildResponse(purchase, purchaseLines)
  }

  fun createDraft(dto: PurchaseCreateDto): PurchaseResponseDto {
    PurchaseValidator.guardNoDuplicateProducts(dto.lines)
    val purchase = PurchaseMapper.toDraftEntity(dto).also { purchaseRepository.save(it) }
    val lines = PurchaseMapper.toLineEntities(purchase.getNullSafeId(), dto.lines)
    purchaseLineRepository.saveAll(lines)
    return purchaseAssembler.buildResponse(purchase, lines)
  }

  fun updateDraft(dto: PurchaseUpdateDto): PurchaseResponseDto {
    val purchase = purchaseRepository.findById(dto.id).orElseThrow { UpdatingNonExistingRecordException() }
    PurchaseValidator.guardIsDraft(purchase)
    PurchaseMapper.applyDraftUpdate(purchase, dto)
    purchaseRepository.save(purchase)
    val lines = applyLineUpdates(purchase.getNullSafeId(), dto)
    PurchaseValidator.guardNoDuplicateProducts(lines)
    return purchaseAssembler.buildResponse(purchase, lines)
  }

  fun createOrder(dto: PurchaseCreateDto): PurchaseResponseDto {
    PurchaseValidator.guardHasLines(dto.lines)
    PurchaseValidator.guardNoDuplicateProducts(dto.lines)
    val purchase = PurchaseMapper.toOrderEntity(dto).also { purchaseRepository.save(it) }
    val lines = PurchaseMapper.toLineEntities(purchase.getNullSafeId(), dto.lines)
    purchaseLineRepository.saveAll(lines)
    return purchaseAssembler.buildResponse(purchase, lines)
  }

  fun convertDraftToOrder(dto: PurchaseUpdateDto): PurchaseResponseDto {
    val purchase = purchaseRepository.findById(dto.id).orElseThrow { UpdatingNonExistingRecordException() }
    PurchaseValidator.guardIsDraft(purchase)
    PurchaseMapper.convertDraftToOrder(purchase, dto)
    purchaseRepository.save(purchase)
    val lines = applyLineUpdates(purchase.getNullSafeId(), dto)
    PurchaseValidator.guardHasLines(lines)
    PurchaseValidator.guardNoDuplicateProducts(lines)
    return purchaseAssembler.buildResponse(purchase, lines)
  }

  fun updateCancelQuantities(id: UUID, lines: List<PurchaseCancelLinesDto>): PurchaseResponseDto {
    val purchase = purchaseRepository.findById(id).orElseThrow { UpdatingNonExistingRecordException() }
    PurchaseValidator.guardCanCancelLines(purchase)
    val existingLines = purchaseLineRepository.findByPurchaseIdIn(listOf(id))
    applyCancelUpdates(existingLines, lines)
    purchase.status = resolveStatusAfterCancellation(existingLines) ?: purchase.status
    purchaseRepository.save(purchase)
    return purchaseAssembler.buildResponse(purchase, existingLines)
  }

  fun updateNotes(id: UUID, notes: String?) {
    notes?.let {
      val purchase = purchaseRepository.findById(id).orElseThrow { UpdatingNonExistingRecordException() }
      purchase.notes = notes
      purchaseRepository.save(purchase)
    }
  }

  private fun applyCancelUpdates(existingLines: List<PurchaseLineEntity>, cancels: List<PurchaseCancelLinesDto>) {
    val linesById = existingLines.associateBy { it.getNullSafeId() }
    val toSave = cancels.mapNotNull { cancel ->
      linesById[cancel.purchaseLineId]?.also { line ->
        PurchaseValidator.guardCancelQuantity(line, cancel)
        line.quantityCanceled = cancel.quantityCanceled
      }
    }
    purchaseLineRepository.saveAll(toSave)
  }

  private fun resolveDeliveryStatus(purchaseLines: List<PurchaseLineEntity>): PurchaseStatus {
    val fullyDelivered = purchaseLines.all {
      it.quantityOrdered - it.quantityCanceled - it.quantityDelivered <= BigDecimal.ZERO
    }
    return if (fullyDelivered) PurchaseStatus.FULLY_DELIVERED else PurchaseStatus.PARTIALLY_DELIVERED
  }

  private fun resolveStatusAfterCancellation(lines: List<PurchaseLineEntity>): PurchaseStatus? {
    val allAccountedFor = lines.all { it.quantityDelivered + it.quantityCanceled == it.quantityOrdered }
    val anyDelivered = lines.any { it.quantityDelivered > BigDecimal.ZERO }
    return when {
      allAccountedFor -> PurchaseStatus.FULLY_DELIVERED
      anyDelivered -> PurchaseStatus.PARTIALLY_DELIVERED
      else -> null
    }
  }

  private fun applyLineUpdates(purchaseId: UUID, dto: PurchaseUpdateDto): List<PurchaseLineEntity> {
    val existingLines = purchaseLineRepository.findByPurchaseIdIn(listOf(purchaseId))
    val linesById = existingLines.associateBy { it.id }
    val toDelete = mutableListOf<PurchaseLineEntity>()
    val toSave = mutableListOf<PurchaseLineEntity>()
    val toCreate = mutableListOf<PurchaseLineEntity>()

    for (lineDto in dto.lines) {
      val existing = lineDto.id.let { linesById[it] }
      if (existing == null) {
        PurchaseValidator.guardNewLineHasProduct(lineDto)
        toCreate.add(PurchaseMapper.toNewLineEntity(purchaseId, lineDto))
      } else if (lineDto.quantityOrdered.compareTo(BigDecimal.ZERO) == 0) {
        toDelete.add(existing)
      } else {
        existing.quantityOrdered = lineDto.quantityOrdered
        existing.unitCost = lineDto.unitCost
        toSave.add(existing)
      }
    }

    purchaseLineRepository.deleteAll(toDelete)
    purchaseLineRepository.saveAll(toSave)
    purchaseLineRepository.saveAll(toCreate)
    val deletedIds = toDelete.mapTo(HashSet()) { it.id }
    val savedIds = toSave.mapTo(HashSet()) { it.id }
    return existingLines.filter { it.id !in deletedIds && it.id !in savedIds } + toSave + toCreate
  }
}
