package me.ezra_home.retail_software_solution.organizations.rest.endpoints

import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchParameters
import me.ezra_home.retail_software_solution.organizations.business.product.OrganizationProductSearchService
import me.ezra_home.retail_software_solution.organizations.business.product.api.OrganizationProductInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product.api.OrganizationProductResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product.api.OrganizationProductService
import me.ezra_home.retail_software_solution.organizations.business.product.api.OrganizationProductUpdateDto
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/products")
class ProductEndpoint(
    private val organizationProductService: OrganizationProductService,
    private val organizationProductSearchService: OrganizationProductSearchService
) {

    @PostMapping
    fun createProduct(@RequestBody productInsertDto: OrganizationProductInsertDto): OrganizationProductResponseDto =
        organizationProductService.createProduct(productInsertDto)

    @PostMapping("search")
    fun search(@RequestBody pageRequest: PageRequest<ProductSearchParameters, String>): PageResponse<OrganizationProductResponseDto, String> =
        organizationProductSearchService.searchWithParameters(pageRequest)

    @PostMapping("search/debug-query")
    fun debugSearchQuery(@RequestBody pageRequest: PageRequest<ProductSearchParameters, String>): String =
        organizationProductSearchService.generateFormattedQuery(pageRequest)

    @PutMapping
    fun updateProduct(@RequestBody productDto: OrganizationProductUpdateDto): OrganizationProductResponseDto =
        organizationProductService.updateProduct(productDto)

    @PutMapping("{orgProductId}/deactivate")
    fun deactivateProduct(@PathVariable orgProductId: UUID): OrganizationProductResponseDto =
        organizationProductService.deactivateProduct(orgProductId)

    @PutMapping("{orgProductId}/reactivate")
    fun reactivateProduct(@PathVariable orgProductId: UUID): OrganizationProductResponseDto =
        organizationProductService.reactivateProduct(orgProductId)

}
