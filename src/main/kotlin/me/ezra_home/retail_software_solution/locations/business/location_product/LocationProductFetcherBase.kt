package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchExecutor
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchParameters
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import me.ezra_home.retail_software_solution.util.queries.FetchesUsingSmartTextStrategy

abstract class LocationProductFetcherBase<T>(
    private val executor: ProductSearchExecutor<LocationProductEntity, T>
) : FetchesUsingSmartTextStrategy<ProductSearchParameters, T> {

    override fun fetch(
        pageRequest: PageRequest<ProductSearchParameters, String>,
        setTimeout: Boolean
    ): PageResponse<T, String> {
        val sqlQuery = LocationProductSearchQueryBuilder.buildSearchQuery(
            pageRequest.parameters,
            pageRequest.previousCursor
        )
        val results = executor.execute(sqlQuery, pageRequest.requestedSize + 1, setTimeout)

        val hasMore = results.size > pageRequest.requestedSize
        val pageResults = if (hasMore) results.take(pageRequest.requestedSize) else results
        val currentCursor = pageResults.lastOrNull()?.let(::cursorFrom) ?: pageRequest.previousCursor

        return PageResponse(
            currentCursor = currentCursor,
            hasMore = hasMore,
            contents = pageResults
        )
    }

    protected abstract fun cursorFrom(row: T): String
}
