package me.ezra_home.retail_software_solution.cross_tier.product.search.organization

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchExecutor
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchParameters
import me.ezra_home.retail_software_solution.organizations.business.product.ProductMapper
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product_tag.mapping.ProductTagQualifier
import me.ezra_home.retail_software_solution.organizations.model.ProductEntity
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import me.ezra_home.retail_software_solution.util.queries.FetchesUsingSmartTextStrategy
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class OrganizationProductFetcher(
    private val productMapper: ProductMapper,
    private val executor: ProductSearchExecutor,
    private val productTagQualifier: ProductTagQualifier
): FetchesUsingSmartTextStrategy<ProductSearchParameters, ProductResponseDto> {

    override fun fetch(
        pageRequest: PageRequest<ProductSearchParameters, String>,
        setTimeout: Boolean
    ): PageResponse<ProductResponseDto, String> {
        val sqlQuery = OrganizationProductQueryBuilder.buildSearchQuery(
            pageRequest.parameters,
            pageRequest.previousCursor
        )
        val results: List<ProductEntity> = executor.executeOrgQuery(sqlQuery, pageRequest.requestedSize + 1, setTimeout)

        val hasMore = results.size > pageRequest.requestedSize
        val pageResults = if (hasMore) results.take(pageRequest.requestedSize) else results
        val dtos: List<ProductResponseDto> = pageResults.map { productMapper.toDtoWithoutTags(it) }
        val contents: Collection<ProductResponseDto> = productTagQualifier.populateTagsForProducts(dtos)
        val currentCursor = contents.lastOrNull()?.productName ?: pageRequest.previousCursor

        return PageResponse(
            currentCursor = currentCursor,
            hasMore = hasMore,
            contents = contents
        )
    }
}
