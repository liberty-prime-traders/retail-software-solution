package me.ezra_home.retail_software_solution.cross_tier.product.search.location

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchExecutor
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchParameters
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductMapper
import me.ezra_home.retail_software_solution.locations.business.location_product.dto.LocationProductResponseDto
import me.ezra_home.retail_software_solution.locations.model.LocationProductEntity
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import me.ezra_home.retail_software_solution.util.queries.FetchesUsingSmartTextStrategy
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema(readOnly = true)
class LocationProductFetcher(
    private val executor: ProductSearchExecutor,
    private val locationProductMapper: LocationProductMapper
) : FetchesUsingSmartTextStrategy<ProductSearchParameters, LocationProductResponseDto>  {

    override fun fetch(
        pageRequest: PageRequest<ProductSearchParameters, String>,
        setTimeout: Boolean
    ): PageResponse<LocationProductResponseDto, String> {
        val sqlQuery = LocationProductSearchQueryBuilder.buildSearchQuery(
            pageRequest.parameters,
            pageRequest.previousCursor
        )
        val results: List<LocationProductEntity> = executor.executeLocationQuery(
            sqlQuery,
            pageRequest.requestedSize + 1,
            setTimeout
        )

        val hasMore = results.size > pageRequest.requestedSize
        val pageResults = if (hasMore) results.take(pageRequest.requestedSize) else results
        val dtos: List<LocationProductResponseDto> = pageResults.map { locationProductMapper.toDto(it) }
        val currentCursor = dtos.lastOrNull()?.productName ?: pageRequest.previousCursor

        return PageResponse(
            currentCursor = currentCursor,
            hasMore = hasMore,
            contents = dtos
        )
    }
}
