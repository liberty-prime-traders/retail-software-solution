package me.ezra_home.retail_software_solution.cross_tier.product.search.organization

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchExecutor
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchParameters
import me.ezra_home.retail_software_solution.organizations.business.product.OrganizationProductMapper
import me.ezra_home.retail_software_solution.organizations.business.product.public.OrganizationProductResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product_tag.mapping.ProductTagQualifier
import me.ezra_home.retail_software_solution.organizations.business.product.OrganizationProductEntity
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import me.ezra_home.retail_software_solution.util.queries.FetchesUsingSmartTextStrategy
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class OrganizationProductFetcher(
    private val organizationProductMapper: OrganizationProductMapper,
    private val executor: ProductSearchExecutor,
    private val productTagQualifier: ProductTagQualifier
): FetchesUsingSmartTextStrategy<ProductSearchParameters, OrganizationProductResponseDto> {

    override fun fetch(
        pageRequest: PageRequest<ProductSearchParameters, String>,
        setTimeout: Boolean
    ): PageResponse<OrganizationProductResponseDto, String> {
        val sqlQuery = OrganizationProductQueryBuilder.buildSearchQuery(
            pageRequest.parameters,
            pageRequest.previousCursor
        )
        val results: List<OrganizationProductEntity> = executor.executeOrgQuery(sqlQuery, pageRequest.requestedSize + 1, setTimeout)

        val hasMore = results.size > pageRequest.requestedSize
        val pageResults = if (hasMore) results.take(pageRequest.requestedSize) else results
        val dtos: List<OrganizationProductResponseDto> = pageResults.map { organizationProductMapper.toDtoWithoutTags(it) }
        val contents: Collection<OrganizationProductResponseDto> = productTagQualifier.populateTagsForProducts(dtos)
        val currentCursor = contents.lastOrNull()?.productName ?: pageRequest.previousCursor

        return PageResponse(
            currentCursor = currentCursor,
            hasMore = hasMore,
            contents = contents
        )
    }
}
