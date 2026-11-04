package me.ezra_home.retail_software_solution.locations.rest.endpoints

import me.ezra_home.retail_software_solution.locations.business.product_purchases_report.api.ProductPurchasesDeliveryLineDto
import me.ezra_home.retail_software_solution.locations.business.product_purchases_report.api.ProductPurchasesDeliveryLinesParameters
import me.ezra_home.retail_software_solution.locations.business.product_purchases_report.api.ProductPurchasesReportParameters
import me.ezra_home.retail_software_solution.locations.business.product_purchases_report.api.ProductPurchasesReportRowDto
import me.ezra_home.retail_software_solution.locations.business.product_purchases_report.api.ProductPurchasesReportService
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/product-purchases-report")
class ProductPurchasesReportEndpoint(
    private val productPurchasesReportService: ProductPurchasesReportService,
) {

    @PostMapping("summary")
    fun summary(
        @RequestBody productPurchasesReportParameters: ProductPurchasesReportParameters
    ): List<ProductPurchasesReportRowDto> {
        return productPurchasesReportService.summarize(productPurchasesReportParameters)
    }

    @PostMapping("{locationProductId}/delivery-lines")
    fun deliveryLines(
        @PathVariable locationProductId: UUID,
        @RequestBody pageRequest: PageRequest<ProductPurchasesDeliveryLinesParameters, String>
    ): PageResponse<ProductPurchasesDeliveryLineDto, String> {
        return productPurchasesReportService.searchDeliveryLines(locationProductId, pageRequest)
    }
}
