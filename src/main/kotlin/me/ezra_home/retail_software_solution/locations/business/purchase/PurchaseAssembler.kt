package me.ezra_home.retail_software_solution.locations.business.purchase

import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryDataFetcher
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryResponseDto
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineResponseDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchasePaymentCeilingService
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
  private val purchaseDeliveryDataFetcher: PurchaseDeliveryDataFetcher,
  private val purchasePaymentCeilingService: PurchasePaymentCeilingService,
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
    val deliveryResponsesByPurchaseId = purchaseDeliveryDataFetcher.getDeliveryResponses(
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
    val deliveries = purchaseDeliveryDataFetcher.getDeliveryResponses(
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
    val deliveredTotal = deliveries.fold(BigDecimal.ZERO) { acc, d -> acc.add(d.deliveryTotal) }
    return PurchaseResponseDto(
      id = purchase.id!!,
      referenceNumber = purchase.requiredReference(),
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
      orderedTotal = lineDtos.fold(BigDecimal.ZERO) { acc, line -> acc.add(line.lineTotal) },
      deliveredTotal = deliveredTotal,
      paymentCeiling = purchasePaymentCeilingService.computeCeiling(lines, deliveredTotal).amount,
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
        referenceNumber = line.requiredReference(),
        quantityOrdered = line.quantityOrdered,
        unitId = line.unitId,
        conversionFactor = line.conversionRatio().factor(),
        unitCost = line.unitCost,
        lineTotal = line.getTotalCost(),
        quantityDelivered = line.quantityDelivered,
        quantityYetToBeDelivered = line.getRemainingQuantity(),
        quantityCanceled = line.quantityCanceled,
        quantityExpected = line.getExpectedQuantity(),
        locationProduct = LocationProductSummaryDto(
          id = product.id,
          referenceNumber = product.referenceNumber,
          productName = product.productName,
          productGroupName = product.productGroupName,
          baseUnitId = product.baseUnitId
        )
      )
    }
  }
}
