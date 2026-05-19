package me.ezra_home.retail_software_solution.locations.rest.endpoints

import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchParameters
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductForSaleDto
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductForSaleDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductResponseDto
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductService
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductUpdateDto
import me.ezra_home.retail_software_solution.locations.business.location_product.api.SaleProductSearchParameters
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockMovementHistoryBuilder
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockMovementResponse
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/location-products")
class LocationProductEndpoint(
  private val locationProductService: LocationProductService,
  private val locationProductDataFetcher: LocationProductDataFetcher,
  private val locationProductForSaleDataFetcher: LocationProductForSaleDataFetcher,
  private val stockMovementHistoryBuilder: StockMovementHistoryBuilder
) {

  @PostMapping("search")
  fun search(
    @RequestBody pageRequest: PageRequest<ProductSearchParameters, String>
  ): PageResponse<LocationProductResponseDto, String> =
    locationProductDataFetcher.searchWithParameters(pageRequest)

  @PostMapping("search-for-sale")
  fun searchForSale(
    @RequestBody pageRequest: PageRequest<SaleProductSearchParameters, String>
  ): PageResponse<LocationProductForSaleDto, String> =
    locationProductForSaleDataFetcher.search(pageRequest)

  @PostMapping("search/debug-query")
  fun debugSearchQuery(
    @RequestBody pageRequest: PageRequest<ProductSearchParameters, String>
  ): String =
    locationProductDataFetcher.generateFormattedQuery(pageRequest)

  @PutMapping
  fun updateProduct(@RequestBody dto: LocationProductUpdateDto): LocationProductResponseDto =
    locationProductService.updateProduct(dto)

  @PutMapping("{productId}/deactivate")
  fun deactivateProduct(@PathVariable productId: UUID): LocationProductResponseDto =
    locationProductService.deactivateProduct(productId)

  @PutMapping("{productId}/reactivate")
  fun reactivateProduct(@PathVariable productId: UUID): LocationProductResponseDto =
    locationProductService.reactivateProduct(productId)

  @GetMapping("history")
  fun getMovementHistory(@RequestParam("locationProductId") locationProductId: UUID): Collection<StockMovementResponse> =
    stockMovementHistoryBuilder.build(locationProductId)
}
