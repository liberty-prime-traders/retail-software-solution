package me.ezra_home.retail_software_solution.locations.rest.endpoints

import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesLineDto
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesLinesParameters
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesReportSearchMetadata
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesReportSearchParameters
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesReportSearchService
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesReportService
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesSummaryDto
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesSummaryParameters
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import me.ezra_home.retail_software_solution.util.paging.PageResponseWithMetadata
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/sales/product-report")
class ProductSalesReportEndpoint(
    private val productSalesReportService: ProductSalesReportService,
    private val productSalesReportSearchService: ProductSalesReportSearchService,
) {

    @PostMapping("{locationProductId}/lines")
    fun lines(
        @PathVariable locationProductId: UUID,
        @RequestBody pageRequest: PageRequest<ProductSalesLinesParameters, String>
    ): PageResponse<ProductSalesLineDto, String> {
        return productSalesReportService.searchLines(locationProductId, pageRequest)
    }

    @PostMapping("summary")
    fun summary(
        @RequestBody productSalesSummaryParameters: ProductSalesSummaryParameters
    ): List<ProductSalesSummaryDto> {
        return productSalesReportService.summarize(productSalesSummaryParameters)
    }

    @PostMapping("search")
    fun search(
        @RequestBody pageRequest: PageRequest<ProductSalesReportSearchParameters, String>
    ): PageResponseWithMetadata<ProductSalesSummaryDto, String, ProductSalesReportSearchMetadata> {
        return productSalesReportSearchService.search(pageRequest)
    }
}
