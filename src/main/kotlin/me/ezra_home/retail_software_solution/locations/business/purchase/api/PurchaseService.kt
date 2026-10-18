package me.ezra_home.retail_software_solution.locations.business.purchase.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductService
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductUnitRequestDto
import me.ezra_home.retail_software_solution.locations.business.purchase.LineUpdateResult
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseAssembler
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseLineRepository
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseLinesResolver
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseMapper
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseRepository
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseValidator
import org.springframework.stereotype.Service
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
  private val purchaseDataFetcher: PurchaseDataFetcher,
) {

  fun createDraft(purchaseCreateDto: PurchaseCreateDto): PurchaseResponseDto {
    PurchaseValidator.guardNoDuplicateProducts(purchaseCreateDto.linesToAdd)
    PurchaseValidator.guardPositiveLineQuantities(purchaseCreateDto.linesToAdd)
    locationProductService.guardAllActive(purchaseCreateDto.linesToAdd.map { it.locationProductId })
    val purchase = PurchaseMapper.toDraftEntity(purchaseCreateDto).also { purchaseRepository.save(it) }
    val lines = PurchaseMapper.toLineEntities(
      purchase.id!!,
      purchaseCreateDto.linesToAdd,
      resolveStorageUnitsPerUnit(purchaseCreateDto.linesToAdd)
    )
    purchaseLineRepository.saveAll(lines)
    return purchaseAssembler.buildResponse(purchase, lines)
  }

  private fun resolveStorageUnitsPerUnit(lines: List<PurchaseLineCreateDto>): Map<UUID, Long> {
    val productUnitRequests = lines.map { LocationProductUnitRequestDto(it.locationProductId, it.unitId) }
    return locationProductDataFetcher.getStorageUnitsPerUnit(productUnitRequests)
  }

  fun updateDraft(purchaseUpdateDto: PurchaseUpdateDto): PurchaseResponseDto {
    val purchase = purchaseDataFetcher.lockAndGetPurchase(purchaseUpdateDto.id)
    PurchaseValidator.guardIsDraft(purchase)
    PurchaseMapper.applyDraftUpdate(purchase, purchaseUpdateDto)
    val lineUpdates = purchaseLinesResolver.detanglePurchaseLines(purchase.id!!, purchaseUpdateDto)
    PurchaseValidator.guardNoDuplicateProducts(lineUpdates.resultingLines)
    purchaseRepository.save(purchase)
    persistLineUpdates(lineUpdates)
    return purchaseAssembler.buildResponse(purchase, lineUpdates.resultingLines)
  }

  fun createOrder(purchaseCreateDto: PurchaseCreateDto): PurchaseResponseDto {
    PurchaseValidator.guardHasLines(purchaseCreateDto.linesToAdd)
    PurchaseValidator.guardNoDuplicateProducts(purchaseCreateDto.linesToAdd)
    PurchaseValidator.guardPositiveLineQuantities(purchaseCreateDto.linesToAdd)
    PurchaseValidator.guardPositiveUnitCosts(purchaseCreateDto.linesToAdd)
    locationProductService.guardAllActive(purchaseCreateDto.linesToAdd.map { it.locationProductId })
    val purchase = PurchaseMapper.toOrderEntity(purchaseCreateDto).also { purchaseRepository.save(it) }
    val lines = PurchaseMapper.toLineEntities(
      purchase.id!!,
      purchaseCreateDto.linesToAdd,
      resolveStorageUnitsPerUnit(purchaseCreateDto.linesToAdd)
    )
    purchaseLineRepository.saveAll(lines)
    return purchaseAssembler.buildResponse(purchase, lines)
  }

  fun convertDraftToOrder(purchaseUpdateDto: PurchaseUpdateDto): PurchaseResponseDto {
    val purchase = purchaseDataFetcher.lockAndGetPurchase(purchaseUpdateDto.id)
    PurchaseValidator.guardIsDraft(purchase)
    PurchaseMapper.convertDraftToOrder(purchase, purchaseUpdateDto)
    val lineUpdates = purchaseLinesResolver.detanglePurchaseLines(purchase.id!!, purchaseUpdateDto)
    PurchaseValidator.guardHasLines(lineUpdates.resultingLines)
    PurchaseValidator.guardNoDuplicateProducts(lineUpdates.resultingLines)
    PurchaseValidator.guardPositiveUnitCosts(lineUpdates.resultingLines)
    purchaseRepository.save(purchase)
    persistLineUpdates(lineUpdates)
    return purchaseAssembler.buildResponse(purchase, lineUpdates.resultingLines)
  }

  private fun persistLineUpdates(result: LineUpdateResult) {
    purchaseLineRepository.deleteAll(result.toDelete)
    purchaseLineRepository.saveAll(result.toUpdate + result.toCreate)
  }
}
