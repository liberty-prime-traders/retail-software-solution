package me.ezra_home.retail_software_solution.locations.business.purchase.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductService
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductUnitRequestDto
import me.ezra_home.retail_software_solution.locations.business.lock.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.locations.business.purchase.LineUpdateResult
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseAssembler
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseLineRepository
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseLinesResolver
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
  private val locationProductService: LocationProductService,
  private val locationProductDataFetcher: LocationProductDataFetcher,
  private val purchaseAssembler: PurchaseAssembler,
  private val purchaseLinesResolver: PurchaseLinesResolver,
  private val entityAdvisoryLock: EntityAdvisoryLock,
) {

  fun createDraft(dto: PurchaseCreateDto): PurchaseResponseDto {
    PurchaseValidator.guardNoDuplicateProducts(dto.linesToAdd)
    locationProductService.guardAllActive(dto.linesToAdd.map { it.locationProductId })
    val purchase = PurchaseMapper.toDraftEntity(dto).also { purchaseRepository.saveAndFlush(it) }
    val lines = PurchaseMapper.toLineEntities(
      purchase.id!!,
      dto.linesToAdd,
      resolveFactors(dto.linesToAdd)
    )
    purchaseLineRepository.saveAll(lines)
    return purchaseAssembler.buildResponse(purchase, lines)
  }

  private fun resolveFactors(lines: List<PurchaseLineCreateDto>): Map<UUID, BigDecimal> {
    val productUnitRequests = lines.map { LocationProductUnitRequestDto(it.locationProductId, it.unitId) }
    return locationProductDataFetcher.getConversionFactors(productUnitRequests)
  }

  fun updateDraft(dto: PurchaseUpdateDto): PurchaseResponseDto {
    entityAdvisoryLock.acquire(LockNamespaces.PURCHASE, dto.id)
    val purchase = purchaseRepository.getReferenceById(dto.id)
    PurchaseValidator.guardIsDraft(purchase)
    PurchaseMapper.applyDraftUpdate(purchase, dto)
    val lineUpdates = purchaseLinesResolver.detanglePurchaseLines(purchase.id!!, dto)
    PurchaseValidator.guardNoDuplicateProducts(lineUpdates.resultingLines)
    purchaseRepository.save(purchase)
    persistLineUpdates(lineUpdates)
    return purchaseAssembler.buildResponse(purchase, lineUpdates.resultingLines)
  }

  fun createOrder(dto: PurchaseCreateDto): PurchaseResponseDto {
    PurchaseValidator.guardHasLines(dto.linesToAdd)
    PurchaseValidator.guardNoDuplicateProducts(dto.linesToAdd)
    locationProductService.guardAllActive(dto.linesToAdd.map { it.locationProductId })
    val purchase = PurchaseMapper.toOrderEntity(dto).also { purchaseRepository.save(it) }
    val lines = PurchaseMapper.toLineEntities(
      purchase.id!!,
      dto.linesToAdd,
      resolveFactors(dto.linesToAdd)
    )
    purchaseLineRepository.saveAll(lines)
    return purchaseAssembler.buildResponse(purchase, lines)
  }

  fun convertDraftToOrder(dto: PurchaseUpdateDto): PurchaseResponseDto {
    entityAdvisoryLock.acquire(LockNamespaces.PURCHASE, dto.id)
    val purchase = purchaseRepository.findById(dto.id).orElseThrow { UpdatingNonExistingRecordException() }
    PurchaseValidator.guardIsDraft(purchase)
    PurchaseMapper.convertDraftToOrder(purchase, dto)
    val lineUpdates = purchaseLinesResolver.detanglePurchaseLines(purchase.id!!, dto)
    PurchaseValidator.guardHasLines(lineUpdates.resultingLines)
    PurchaseValidator.guardNoDuplicateProducts(lineUpdates.resultingLines)
    purchaseRepository.save(purchase)
    persistLineUpdates(lineUpdates)
    return purchaseAssembler.buildResponse(purchase, lineUpdates.resultingLines)
  }

  private fun persistLineUpdates(result: LineUpdateResult) {
    purchaseLineRepository.deleteAll(result.toDelete)
    purchaseLineRepository.saveAll(result.toUpdate + result.toCreate)
  }
}
