package me.ezra_home.retail_software_solution.locations.rest.endpoints.delivery

import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryService
import me.ezra_home.retail_software_solution.locations.business.delivery.dto.PurchaseDeliveryCreateDto
import me.ezra_home.retail_software_solution.locations.business.purchase.dto.PurchaseResponseDto
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/deliveries")
class PurchaseDeliveryEndpoint(
  private val purchaseDeliveryService: PurchaseDeliveryService
) {

  @PostMapping
  fun recordDelivery(@RequestBody dto: PurchaseDeliveryCreateDto): PurchaseResponseDto =
    purchaseDeliveryService.recordDelivery(dto)
}
