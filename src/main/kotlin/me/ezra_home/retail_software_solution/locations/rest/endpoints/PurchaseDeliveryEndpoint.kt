package me.ezra_home.retail_software_solution.locations.rest.endpoints

import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryCreateDto
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryService
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseResponseDto
import me.ezra_home.retail_software_solution.locations.business.stock_revision.api.DeliveryLineRevisionPreviewDto
import me.ezra_home.retail_software_solution.locations.business.stock_revision.api.DeliveryLineRevisionPreviewer
import me.ezra_home.retail_software_solution.locations.business.stock_revision.api.DeliveryLineRevisionRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/deliveries")
class PurchaseDeliveryEndpoint(
  private val purchaseDeliveryService: PurchaseDeliveryService,
  private val deliveryLineRevisionPreviewer: DeliveryLineRevisionPreviewer
) {

  @PostMapping("line-revisions/preview")
  fun previewLineRevision(
    @RequestBody deliveryLineRevisionRequest: DeliveryLineRevisionRequest
  ): DeliveryLineRevisionPreviewDto = deliveryLineRevisionPreviewer.preview(deliveryLineRevisionRequest)


  @PostMapping
  fun recordDelivery(@RequestBody dto: PurchaseDeliveryCreateDto): PurchaseResponseDto =
    purchaseDeliveryService.recordDelivery(dto)
}
