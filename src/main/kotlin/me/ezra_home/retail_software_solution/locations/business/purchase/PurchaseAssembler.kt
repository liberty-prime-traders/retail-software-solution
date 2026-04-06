package me.ezra_home.retail_software_solution.locations.business.purchase

import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryFetcher
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryResponseDto
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductService
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineProductDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineResponseDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseResponseDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueService
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class PurchaseAssembler(
  private val purchaseLineRepository: PurchaseLineRepository,
  private val locationProductService: LocationProductService,
  private val purchaseDeliveryFetcher: PurchaseDeliveryFetcher,
  private val contactService: ContactService,
  private val unitValueService: UnitValueService,
  private val userQualifier: UserQualifier
) {

  fun buildResponses(purchases: List<PurchaseEntity>): List<PurchaseResponseDto> {
    val purchaseIds = purchases.map { it.getNullSafeId() }
    val allLines = purchaseLineRepository.findByPurchaseIdIn(purchaseIds)
    val productSummaries = loadProductSummaries(allLines.map { it.locationProductId })
    val linesByPurchaseId = allLines.groupBy { it.purchaseId }
    val purchaseLineDtoMap = allLines.associateBy { it.getNullSafeId() }.mapValues { it.value.toLineDto() }
    val deliveryResponsesByPurchaseId = purchaseDeliveryFetcher.getDeliveryResponses(purchaseIds, purchaseLineDtoMap, productSummaries)
    val supplierNameMap = contactService.getAllContactDtos().associateBy({ it.id }, { it.identity.displayName })
    return purchases.map { purchase ->
      val lines = linesByPurchaseId[purchase.id] ?: emptyList()
      val deliveries = deliveryResponsesByPurchaseId[purchase.id] ?: emptyList()
      buildResponse(purchase, lines, productSummaries, supplierNameMap, deliveries)
    }
  }

  fun buildResponse(purchase: PurchaseEntity, lines: List<PurchaseLineEntity>): PurchaseResponseDto {
    val productSummaries = loadProductSummaries(lines.map { it.locationProductId })
    val purchaseLineDtoMap = lines.associateBy { it.getNullSafeId() }.mapValues { it.value.toLineDto() }
    val deliveries = purchaseDeliveryFetcher.getDeliveryResponses(
        listOf(purchase.getNullSafeId()), purchaseLineDtoMap, productSummaries
    )[purchase.id] ?: emptyList()
    val supplierNameMap = contactService.getAllContactDtos().associateBy({ it.id }, { it.identity.displayName })
    return buildResponse(purchase, lines, productSummaries, supplierNameMap, deliveries)
  }

  private fun buildResponse(
    purchase: PurchaseEntity,
    lines: List<PurchaseLineEntity>,
    productSummaries: Map<UUID, LocationProductSummaryDto>,
    supplierNameMap: Map<UUID?, String>,
    deliveries: List<PurchaseDeliveryResponseDto>
  ): PurchaseResponseDto {
    val lineDtos = toLinesDto(lines, productSummaries)
    val orderTotal = lineDtos.fold(BigDecimal.ZERO) { acc, line -> acc.add(line.lineTotal) }
    return PurchaseResponseDto(
      id = purchase.getNullSafeId(),
      referenceNumber = purchase.getNullSafeReferenceNumber(),
      supplierId = purchase.supplierId,
      supplierName = supplierNameMap[purchase.supplierId],
      status = purchase.status,
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

  private fun loadProductSummaries(ids: List<UUID>): Map<UUID, LocationProductSummaryDto> =
      locationProductService.findSummaryByIds(ids)

  private fun toLinesDto(lines: List<PurchaseLineEntity>, productSummaries: Map<UUID, LocationProductSummaryDto>): List<PurchaseLineResponseDto> {
    return lines.map { line ->
      val product = productSummaries[line.locationProductId]!!
      val quantityExpected = line.quantityOrdered.subtract(line.quantityCanceled)
      PurchaseLineResponseDto(
        id = line.getNullSafeId(),
        referenceNumber = line.getNullSafeReferenceNumber(),
        locationProduct = PurchaseLineProductDto(
          referenceNumber = product.getNullSafeReferenceNumber(),
          productName = product.productName,
          productGroupName = product.productGroupName,
          baseUnit = unitValueService.getUnitName(product.baseUnitId)
        ),
        quantityOrdered = line.quantityOrdered,
        unitCost = line.unitCost,
        lineTotal = quantityExpected.multiply(line.unitCost),
        quantityDelivered = line.quantityDelivered,
        quantityYetToBeDelivered = quantityExpected.subtract(line.quantityDelivered),
        quantityCanceled = line.quantityCanceled,
        quantityExpected = quantityExpected
      )
    }
  }

  private fun PurchaseLineEntity.toLineDto() = PurchaseLineDto(
      id = getNullSafeId(),
      purchaseId = purchaseId,
      locationProductId = locationProductId,
      quantityOrdered = quantityOrdered,
      unitCost = unitCost,
      quantityDelivered = quantityDelivered,
      quantityCanceled = quantityCanceled
  )
}
