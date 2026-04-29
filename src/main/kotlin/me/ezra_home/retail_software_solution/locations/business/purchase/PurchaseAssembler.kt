package me.ezra_home.retail_software_solution.locations.business.purchase

import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryFetcher
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryResponseDto
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineProductDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineResponseDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseResponseDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class PurchaseAssembler(
  private val purchaseLineRepository: PurchaseLineRepository,
  private val locationProductDataFetcher: LocationProductDataFetcher,
  private val purchaseDeliveryFetcher: PurchaseDeliveryFetcher,
  private val contactService: ContactService,
  private val userQualifier: UserQualifier
) {

  fun buildResponses(purchases: List<PurchaseEntity>): List<PurchaseResponseDto> {
    val purchaseIds = purchases.map { it.id!! }
    val allLines = purchaseLineRepository.findByPurchaseIdIn(purchaseIds)
    val productSummaries = locationProductDataFetcher.findSummaryByIds(allLines.map { it.locationProductId })
    val linesByPurchaseId = allLines.groupBy { it.purchaseId }
    val purchaseLineDtoMap = allLines.associateBy { it.id!! }.mapValues {
      PurchaseMapper.purchaseLineEntityToDto(it.value)
    }
    val deliveryResponsesByPurchaseId = purchaseDeliveryFetcher.getDeliveryResponses(
      purchaseIds, purchaseLineDtoMap, productSummaries
    )
    val supplierNameMap = contactService.getAllContactDtos().associateBy(
      { it.id }, { it.identity.displayName }
    )
    return purchases.map { purchase ->
      val lines = linesByPurchaseId[purchase.id] ?: emptyList()
      val deliveries = deliveryResponsesByPurchaseId[purchase.id] ?: emptyList()
      buildResponse(purchase, lines, productSummaries, supplierNameMap, deliveries)
    }
  }

  fun buildResponse(purchase: PurchaseEntity, lines: List<PurchaseLineEntity>): PurchaseResponseDto {
    val productSummaries = locationProductDataFetcher.findSummaryByIds(lines.map { it.locationProductId })
    val purchaseLineDtoMap = lines.associateBy { it.id!! }.mapValues {
      PurchaseMapper.purchaseLineEntityToDto(it.value)
    }
    val deliveries = purchaseDeliveryFetcher.getDeliveryResponses(
        listOf(purchase.id!!), purchaseLineDtoMap, productSummaries
    )[purchase.id] ?: emptyList()
    val supplierNameMap = contactService.getAllContactDtos().associateBy(
      { it.id }, { it.identity.displayName }
    )
    return buildResponse(purchase, lines, productSummaries, supplierNameMap, deliveries)
  }

  private fun buildResponse(
    purchase: PurchaseEntity,
    lines: List<PurchaseLineEntity>,
    productSummaries: Map<UUID, LocationProductSummaryDto>,
    supplierNameMap: Map<UUID, String>,
    deliveries: List<PurchaseDeliveryResponseDto>
  ): PurchaseResponseDto {
    val lineDtos = toLinesDto(lines, productSummaries)
    val orderTotal = lineDtos.fold(BigDecimal.ZERO) { acc, line -> acc.add(line.lineTotal) }
    return PurchaseResponseDto(
      id = purchase.id!!,
      referenceNumber = purchase.referenceNumber!!,
      supplierId = purchase.supplierId,
      supplierName = supplierNameMap[purchase.supplierId],
      purchaseStatus = purchase.purchaseStatus,
      paymentStatus = purchase.paymentStatus,
      notes = purchase.notes,
      dateOrdered = purchase.dateOrdered,
      orderedById = purchase.orderedById,
      orderedBy = userQualifier.getUserFullName(purchase.orderedById),
      createdBy = userQualifier.getUserFullName(purchase.createdById),
      createdOn = purchase.createdOn,
      lines = lineDtos,
      orderTotal = orderTotal,
      deliveries = deliveries
    )
  }

  private fun toLinesDto(
    lines: List<PurchaseLineEntity>,
    productSummaries: Map<UUID, LocationProductSummaryDto>
  ): List<PurchaseLineResponseDto> {
    return lines.map { line ->
      val product = productSummaries[line.locationProductId]!!
      PurchaseLineResponseDto(
        id = line.id!!,
        referenceNumber = line.referenceNumber!!,
        quantityOrdered = line.quantityOrdered,
        unitId = line.unitId,
        conversionFactor = line.conversionFactor,
        unitCost = line.unitCost,
        lineTotal = line.getTotalCost(),
        quantityDelivered = line.quantityDelivered,
        quantityYetToBeDelivered = line.getRemainingQuantity(),
        quantityCanceled = line.quantityCanceled,
        quantityExpected = line.getExpectedQuantity(),
        locationProduct = PurchaseLineProductDto(
          referenceNumber = product.referenceNumber,
          productName = product.productName,
          productGroupName = product.productGroupName,
          baseUnitId = product.baseUnitId
        )
      )
    }
  }
}
