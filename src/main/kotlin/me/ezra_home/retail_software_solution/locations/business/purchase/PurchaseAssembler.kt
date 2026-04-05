package me.ezra_home.retail_software_solution.locations.business.purchase

import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryAssembler
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryRepository
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductRepository
import me.ezra_home.retail_software_solution.locations.business.purchase.dto.PurchaseLineProductDto
import me.ezra_home.retail_software_solution.locations.business.purchase.dto.PurchaseLineResponseDto
import me.ezra_home.retail_software_solution.locations.business.purchase.dto.PurchaseResponseDto
import me.ezra_home.retail_software_solution.locations.model.LocationProductEntity
import me.ezra_home.retail_software_solution.locations.model.PurchaseDeliveryEntity
import me.ezra_home.retail_software_solution.locations.model.PurchaseEntity
import me.ezra_home.retail_software_solution.locations.model.PurchaseLineEntity
import me.ezra_home.retail_software_solution.organizations.business.contact.ContactCache
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitValueQualifier
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
internal class PurchaseAssembler(
  private val locationProductRepository: LocationProductRepository,
  private val purchaseLineRepository: PurchaseLineRepository,
  private val purchaseDeliveryRepository: PurchaseDeliveryRepository,
  private val purchaseDeliveryAssembler: PurchaseDeliveryAssembler,
  private val contactCache: ContactCache,
  private val unitValueQualifier: UnitValueQualifier,
  private val userQualifier: UserQualifier
) {

  fun buildResponses(purchases: List<PurchaseEntity>): List<PurchaseResponseDto> {
    val purchaseIds = purchases.map { it.getNullSafeId() }
    val allLines = purchaseLineRepository.findByPurchaseIdIn(purchaseIds)
    val productMap = loadProductMap(allLines.map { it.locationProductId })
    val linesByPurchaseId = allLines.groupBy { it.purchaseId }
    val allDeliveries = purchaseDeliveryRepository.findByPurchaseIdIn(purchaseIds)
    val deliveriesByPurchaseId = allDeliveries.groupBy { it.purchaseId }
    return purchases.map { purchase ->
      val lines = linesByPurchaseId[purchase.id] ?: emptyList()
      val deliveries = deliveriesByPurchaseId[purchase.id] ?: emptyList()
      buildResponse(purchase, lines, productMap, deliveries)
    }
  }

  fun buildResponse(purchase: PurchaseEntity, lines: List<PurchaseLineEntity>): PurchaseResponseDto {
    val deliveries = purchaseDeliveryRepository.findByPurchaseIdIn(listOf(purchase.getNullSafeId()))
    return buildResponse(purchase, lines, loadProductMap(lines.map { it.locationProductId }), deliveries)
  }

  private fun buildResponse(
    purchase: PurchaseEntity,
    lines: List<PurchaseLineEntity>,
    productMap: Map<UUID?, LocationProductEntity>,
    deliveries: List<PurchaseDeliveryEntity>
  ): PurchaseResponseDto {
    val supplierNameMap = contactCache.getAllContacts().associateBy({ it.id }, { it.identity.displayName })
    val lineDtos = toLinesDto(lines, productMap)
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
      deliveries = purchaseDeliveryAssembler.buildResponses(deliveries, lines, productMap)
    )
  }

  private fun loadProductMap(ids: List<UUID>): Map<UUID?, LocationProductEntity> {
    return locationProductRepository.findAllById(ids).associateBy { it.id }
  }

  private fun toLinesDto(lines: List<PurchaseLineEntity>, productMap: Map<UUID?, LocationProductEntity>): List<PurchaseLineResponseDto> {
    return lines.map { line ->
      val product = productMap[line.locationProductId]!!
      val quantityExpected = line.quantityOrdered.subtract(line.quantityCanceled)
      PurchaseLineResponseDto(
        id = line.getNullSafeId(),
        referenceNumber = line.getNullSafeReferenceNumber(),
        locationProduct = PurchaseLineProductDto(
          referenceNumber = product.getNullSafeReferenceNumber(),
          productName = product.productName,
          productGroupName = product.productGroupName,
          baseUnit = unitValueQualifier.getUnitName(product.baseUnitId)
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
}
