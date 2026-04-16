package me.ezra_home.retail_software_solution.locations.rest.endpoints

import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryCreateDto
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryService
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseResponseDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/deliveries")
class PurchaseDeliveryEndpoint(
  private val purchaseDeliveryService: PurchaseDeliveryService
) {

  @PostMapping
  fun recordDelivery(@RequestBody dto: PurchaseDeliveryCreateDto): PurchaseResponseDto =
    purchaseDeliveryService.recordDelivery(dto)

  @PostMapping("/{id}/republish")
  fun republishEvent(@PathVariable id: UUID): ResponseEntity<Void> {
    purchaseDeliveryService.republishEvent(id)
    return ResponseEntity.ok().build()
  }
}
