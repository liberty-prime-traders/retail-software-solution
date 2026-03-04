package me.ezra_home.retail_software_solution.locations.business.purchase

import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductMapper
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductRepository
import me.ezra_home.retail_software_solution.locations.business.purchase.dto.PurchaseLineResponseDto
import me.ezra_home.retail_software_solution.locations.business.purchase.dto.PurchaseResponseDto
import me.ezra_home.retail_software_solution.locations.model.LocationProductEntity
import me.ezra_home.retail_software_solution.locations.model.PurchaseEntity
import me.ezra_home.retail_software_solution.locations.model.PurchaseLineEntity
import me.ezra_home.retail_software_solution.organizations.business.contact.ContactCache
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class PurchaseAssembler(
  private val locationProductRepository: LocationProductRepository,
  private val purchaseLineRepository: PurchaseLineRepository,
  private val contactCache: ContactCache,
  private val locationProductMapper: LocationProductMapper,
  private val userQualifier: UserQualifier
) {

  fun buildResponses(purchases: List<PurchaseEntity>): List<PurchaseResponseDto> {
    val allLines = purchaseLineRepository.findByPurchaseIdIn(purchases.map { it.id!! })
    val productMap = loadProductMap(allLines.map { it.locationProductId })
    val linesByPurchaseId = allLines.groupBy { it.purchaseId }
    return purchases.map { buildResponse(it, linesByPurchaseId[it.id] ?: emptyList(), productMap) }
  }

  private fun loadProductMap(ids: List<UUID>): Map<UUID?, LocationProductEntity> {
    return locationProductRepository.findAllById(ids).associateBy { it.id }
  }

  fun buildResponse(purchase: PurchaseEntity, lines: List<PurchaseLineEntity>): PurchaseResponseDto {
    return buildResponse(purchase, lines, loadProductMap(lines.map { it.locationProductId }))
  }

  fun buildResponse(
    purchase: PurchaseEntity,
    lines: List<PurchaseLineEntity>,
    productMap: Map<UUID?, LocationProductEntity>
  ): PurchaseResponseDto {
    val supplierNameMap = contactCache.getAllContacts().associateBy({ it.id }, { it.identity.displayName })
    val lineDtos = toLinesDto(lines, productMap)
    val orderTotal = lineDtos.fold(BigDecimal.ZERO) { acc, line -> acc.add(line.lineTotal) }

    return PurchaseResponseDto(
      id = purchase.id,
      referenceNumber = purchase.referenceNumber,
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
      orderTotal = orderTotal
    )
  }

  private fun toLinesDto(lines: List<PurchaseLineEntity>, productMap: Map<UUID?, LocationProductEntity>): List<PurchaseLineResponseDto> {
    return lines.map { line ->
      val quantityExpected = line.quantityOrdered.subtract(line.quantityDelivered).subtract(line.quantityCanceled)
      val lineTotal = quantityExpected.multiply(line.unitCost)
      PurchaseLineResponseDto(
        id = line.id,
        referenceNumber = line.referenceNumber,
        locationProduct = productMap[line.locationProductId]?.let { locationProductMapper.toDto(it) },
        quantityOrdered = line.quantityOrdered,
        unitCost = line.unitCost,
        lineTotal = lineTotal,
        quantityDelivered = line.quantityDelivered,
        quantityCanceled = line.quantityCanceled,
        quantityExpected = quantityExpected
      )
    }
  }
}
