package me.ezra_home.retail_software_solution.organizations.business.product.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchParameters
import me.ezra_home.retail_software_solution.cross_tier.product.search.organization.OrganizationProductQueryBuilder
import me.ezra_home.retail_software_solution.organizations.business.product.OrganizationProductCache
import me.ezra_home.retail_software_solution.organizations.business.product.OrganizationProductMapper
import me.ezra_home.retail_software_solution.organizations.business.product.OrganizationProductSearchExecutor
import me.ezra_home.retail_software_solution.organizations.business.product_tag.mapping.ProductTagQualifier
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueService
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import me.ezra_home.retail_software_solution.util.queries.FetchesUsingSmartTextStrategy
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class OrganizationProductFetcher(
    private val executor: OrganizationProductSearchExecutor,
    private val productTagQualifier: ProductTagQualifier,
    private val unitValueService: UnitValueService,
    private val organizationProductCache: OrganizationProductCache,
    private val organizationProductMapper: OrganizationProductMapper
): FetchesUsingSmartTextStrategy<ProductSearchParameters, OrganizationProductResponseDto> {

    override fun fetch(
        pageRequest: PageRequest<ProductSearchParameters, String>,
        setTimeout: Boolean
    ): PageResponse<OrganizationProductResponseDto, String> {
        val sqlQuery = OrganizationProductQueryBuilder.buildSearchQuery(
            pageRequest.parameters,
            pageRequest.previousCursor
        )
        val results = executor.execute(sqlQuery, pageRequest.requestedSize + 1, setTimeout)

        val hasMore = results.size > pageRequest.requestedSize
        val pageResults = if (hasMore) results.take(pageRequest.requestedSize) else results
        val contents = productTagQualifier.populateTagsForProducts(pageResults)
        val currentCursor = contents.lastOrNull()?.productName ?: pageRequest.previousCursor

        return PageResponse(
            currentCursor = currentCursor,
            hasMore = hasMore,
            contents = contents
        )
    }

    fun findAllProducts(): List<OrganizationProductResponseDto> {
        val unitNamesById = unitValueService.getUnitNamesById()
        val responseDtos = organizationProductCache.findAllProducts()
            .map { organizationProductMapper.toResponseDtoWithoutTags(it, unitNamesById[it.baseUnitId]) }
        return productTagQualifier.populateTagsForProducts(responseDtos)
    }

    fun countAllProducts(): Long = organizationProductCache.countAllProducts()
}
