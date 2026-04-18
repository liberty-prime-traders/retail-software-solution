package me.ezra_home.retail_software_solution.locations.business.purchase.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductRepository
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseAssembler
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseLineEntity
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseLineRepository
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseMapper
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseRepository
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseValidator
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID


@Service
@TransactionalOnLocationSchema
class PurchaseService(
  private val purchaseRepository: PurchaseRepository,
  private val purchaseLineRepository: PurchaseLineRepository,
  private val locationProductRepository: LocationProductRepository,
  private val purchaseAssembler: PurchaseAssembler
) {

  fun createDraft(dto: PurchaseCreateDto): PurchaseResponseDto {
    PurchaseValidator.guardNoDuplicateProducts(dto.lines)
    guardNoInactiveProducts(dto.lines.map { it.locationProductId })
    val purchase = PurchaseMapper.toDraftEntity(dto).also { purchaseRepository.saveAndFlush(it) }
    val lines = PurchaseMapper.toLineEntities(purchase.id!!, dto.lines)
    purchaseLineRepository.saveAll(lines)
    return purchaseAssembler.buildResponse(purchase, lines)
  }

  fun updateDraft(dto: PurchaseUpdateDto): PurchaseResponseDto {
    val purchase = purchaseRepository.findById(dto.id).orElseThrow { UpdatingNonExistingRecordException() }
    PurchaseValidator.guardIsDraft(purchase)
    PurchaseMapper.applyDraftUpdate(purchase, dto)
    purchaseRepository.save(purchase)
    val lines = applyLineUpdates(purchase.id!!, dto)
    PurchaseValidator.guardNoDuplicateProducts(lines)
    return purchaseAssembler.buildResponse(purchase, lines)
  }

  fun createOrder(dto: PurchaseCreateDto): PurchaseResponseDto {
    PurchaseValidator.guardHasLines(dto.lines)
    PurchaseValidator.guardNoDuplicateProducts(dto.lines)
    guardNoInactiveProducts(dto.lines.map { it.locationProductId })
    val purchase = PurchaseMapper.toOrderEntity(dto).also { purchaseRepository.save(it) }
    val lines = PurchaseMapper.toLineEntities(purchase.id!!, dto.lines)
    purchaseLineRepository.saveAll(lines)
    return purchaseAssembler.buildResponse(purchase, lines)
  }

  fun convertDraftToOrder(dto: PurchaseUpdateDto): PurchaseResponseDto {
    val purchase = purchaseRepository.findById(dto.id).orElseThrow { UpdatingNonExistingRecordException() }
    PurchaseValidator.guardIsDraft(purchase)
    PurchaseMapper.convertDraftToOrder(purchase, dto)
    purchaseRepository.save(purchase)
    val lines = applyLineUpdates(purchase.id!!, dto)
    PurchaseValidator.guardHasLines(lines)
    PurchaseValidator.guardNoDuplicateProducts(lines)
    return purchaseAssembler.buildResponse(purchase, lines)
  }

  fun updateCancelQuantities(id: UUID, lines: List<PurchaseCancelLinesDto>): PurchaseResponseDto {
    val purchase = purchaseRepository.findById(id).orElseThrow { UpdatingNonExistingRecordException() }
    PurchaseValidator.guardCanCancelLines(purchase)
    val existingLines = purchaseLineRepository.findByPurchaseId(id)
    applyCancelUpdates(existingLines, lines)
    purchase.purchaseStatus = resolveStatusAfterCancellation(existingLines) ?: purchase.purchaseStatus
    purchaseRepository.save(purchase)
    return purchaseAssembler.buildResponse(purchase, existingLines)
  }

  private fun applyCancelUpdates(existingLines: List<PurchaseLineEntity>, cancels: List<PurchaseCancelLinesDto>) {
    val linesById = existingLines.associateBy { it.id!! }
    val toSave = cancels.mapNotNull { cancel ->
      linesById[cancel.purchaseLineId]?.also { line ->
        PurchaseValidator.guardCancelQuantity(line, cancel)
        line.quantityCanceled = cancel.quantityCanceled
      }
    }
    purchaseLineRepository.saveAll(toSave)
  }

  fun updateNotes(id: UUID, notes: String?) {
    notes?.let {
      val purchase = purchaseRepository.findById(id).orElseThrow { UpdatingNonExistingRecordException() }
      purchase.notes = notes
      purchaseRepository.save(purchase)
    }
  }

  private fun guardNoInactiveProducts(productIds: List<UUID>) {
    val products = locationProductRepository.findAllById(productIds)
    PurchaseValidator.guardNoInactiveProducts(products)
  }

  fun updatePaymentStatus(purchaseId: UUID, status: PaymentStatus) {
    val purchase = purchaseRepository.getReferenceById(purchaseId)
    purchase.paymentStatus = status
    purchaseRepository.save(purchase)
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

    val newProductIds = dto.lines.filter { linesById[it.id] == null }.map { it.locationProductId }
    guardNoInactiveProducts(newProductIds)

    for (lineDto in dto.lines) {
      val existing = lineDto.id.let { linesById[it] }
      if (existing == null) {
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
