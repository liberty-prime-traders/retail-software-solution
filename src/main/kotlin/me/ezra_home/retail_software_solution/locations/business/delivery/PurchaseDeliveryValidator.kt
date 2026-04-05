package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.locations.business.delivery.dto.PurchaseDeliveryCreateDto
import me.ezra_home.retail_software_solution.locations.model.PurchaseLineEntity
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.math.BigDecimal
import java.util.UUID

internal object PurchaseDeliveryValidator {

  fun validate(dto: PurchaseDeliveryCreateDto, purchaseLineById: Map<UUID, PurchaseLineEntity>) {
    if (dto.lines.isEmpty())
      throw RtsGenericException("Delivery must have at least one line.")

    val lineIds = dto.lines.map { it.purchaseLineId }
    if (lineIds.size != lineIds.toSet().size)
      throw RtsGenericException("Duplicate purchase line IDs in delivery request.")

    dto.lines.forEach { lineDto ->
      if (lineDto.quantityDelivered <= BigDecimal.ZERO)
        throw RtsGenericException("Quantity delivered must be greater than zero for line ${lineDto.purchaseLineId}.")

      if (lineDto.unitCost <= BigDecimal.ZERO)
        throw RtsGenericException("Unit cost must be greater than zero for line ${lineDto.purchaseLineId}.")

      val purchaseLine = purchaseLineById[lineDto.purchaseLineId]
        ?: throw RtsGenericException("Purchase line ${lineDto.purchaseLineId} does not belong to this purchase.")

      val remaining = purchaseLine.quantityOrdered - purchaseLine.quantityCanceled - purchaseLine.quantityDelivered
      if (lineDto.quantityDelivered > remaining)
        throw RtsGenericException(
          "Maximum quantity that can be delivered for line ${lineDto.purchaseLineId} is $remaining." +
                  " However, ${lineDto.quantityDelivered} was provided."
        )
    }
  }
}
