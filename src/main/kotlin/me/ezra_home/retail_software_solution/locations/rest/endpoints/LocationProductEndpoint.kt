package me.ezra_home.retail_software_solution.locations.rest.endpoints

import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchParameters
import me.ezra_home.retail_software_solution.cross_tier.product.search.location.LocationProductSearchService
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductResponseDto
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductService
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductUpdateDto
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
@RequestMapping("secured/location-products")
class LocationProductEndpoint(
  private val locationProductService: LocationProductService,
  private val locationProductSearchService: LocationProductSearchService
) {

  @PostMapping("search")
  fun search(
    @RequestBody pageRequest: PageRequest<ProductSearchParameters, String>
  ): PageResponse<LocationProductResponseDto, String> =
    locationProductSearchService.searchWithParameters(pageRequest)

  @PostMapping("search/debug-query")
  fun debugSearchQuery(
    @RequestBody pageRequest: PageRequest<ProductSearchParameters, String>
  ): String =
    locationProductSearchService.generateFormattedQuery(pageRequest)

  @PutMapping
  fun updateProduct(@RequestBody dto: LocationProductUpdateDto): LocationProductResponseDto =
    locationProductService.updateProduct(dto)

  @PutMapping("{productId}/deactivate")
  fun deactivateProduct(@PathVariable productId: UUID): LocationProductResponseDto =
    locationProductService.deactivateProduct(productId)

  @PutMapping("{productId}/reactivate")
  fun reactivateProduct(@PathVariable productId: UUID): LocationProductResponseDto =
    locationProductService.reactivateProduct(productId)
}
