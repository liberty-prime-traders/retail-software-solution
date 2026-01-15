package me.ezra_home.retail_software_solution.organizations.business.product.search

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.product.ProductMapper
import me.ezra_home.retail_software_solution.organizations.business.product.dto.ProductResponseDto
import me.ezra_home.retail_software_solution.organizations.model.ProductEntity
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class ProductSearchService(
  private val queryBuilder: ProductSearchQueryBuilder,
  private val productMapper: ProductMapper
) {

  fun searchWithParameters(pageRequest: PageRequest<ProductSearchParameters>): PageResponse<ProductResponseDto> {
    val params = pageRequest.parameters
    val pageSize = pageRequest.requestedSize

    val results: List<ProductEntity> = queryBuilder.buildAndExecuteQuery(
      productName = params.productName?.takeIf { it.isNotBlank() },
      description = params.description?.takeIf { it.isNotBlank() },
      referenceNumber = params.referenceNumber?.takeIf { it.isNotBlank() },
      categoryIds = params.categoryIds,
      tagIds = params.tagsIds,
      statusList = params.statusList.map { it.code }.toSet(),
      previousCursor = pageRequest.previousCursor,
      pageSize = pageSize + 1
    )

    val hasMore = results.size > pageSize
    val contents: Collection<ProductResponseDto> = results.map { productMapper.toDto(it) }
    val currentCursor = contents.lastOrNull()?.cursor ?: pageRequest.previousCursor

    return PageResponse(
      currentCursor = currentCursor,
      hasMore = hasMore,
      contents = contents
    )
  }
}
