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
import me.ezra_home.retail_software_solution.util.business.ConversionRatio
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

  fun createDraft(dto: PurchaseCreateDto): PurchaseResponseDto {
    PurchaseValidator.guardNoDuplicateProducts(dto.linesToAdd)
    PurchaseValidator.guardPositiveLineQuantities(dto.linesToAdd)
    locationProductService.guardAllActive(dto.linesToAdd.map { it.locationProductId })
    val purchase = PurchaseMapper.toDraftEntity(dto).also { purchaseRepository.saveAndFlush(it) }
    val lines = PurchaseMapper.toLineEntities(
      purchase.id!!,
      dto.linesToAdd,
      resolveConversionRatios(dto.linesToAdd)
    )
    purchaseLineRepository.saveAll(lines)
    return purchaseAssembler.buildResponse(purchase, lines)
  }

  private fun resolveConversionRatios(lines: List<PurchaseLineCreateDto>): Map<UUID, ConversionRatio> {
    val productUnitRequests = lines.map { LocationProductUnitRequestDto(it.locationProductId, it.unitId) }
    return locationProductDataFetcher.getConversionRatios(productUnitRequests)
  }

  fun updateDraft(dto: PurchaseUpdateDto): PurchaseResponseDto {
    val purchase = purchaseDataFetcher.lockAndGetPurchase(dto.id)
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
    PurchaseValidator.guardPositiveLineQuantities(dto.linesToAdd)
    locationProductService.guardAllActive(dto.linesToAdd.map { it.locationProductId })
    val purchase = PurchaseMapper.toOrderEntity(dto).also { purchaseRepository.save(it) }
    val lines = PurchaseMapper.toLineEntities(
      purchase.id!!,
      dto.linesToAdd,
      resolveConversionRatios(dto.linesToAdd)
    )
    purchaseLineRepository.saveAll(lines)
    return purchaseAssembler.buildResponse(purchase, lines)
  }

  fun convertDraftToOrder(dto: PurchaseUpdateDto): PurchaseResponseDto {
    val purchase = purchaseDataFetcher.lockAndGetPurchase(dto.id)
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
