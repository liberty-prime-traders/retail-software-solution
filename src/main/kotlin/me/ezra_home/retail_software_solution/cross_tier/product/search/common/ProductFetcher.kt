package me.ezra_home.retail_software_solution.cross_tier.product.search.common

import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse

fun interface ProductFetcher<T> {
  fun fetchProducts(
    pageRequest: PageRequest<ProductSearchParameters, String>,
    setTimeout: Boolean
  ): PageResponse<T, String>
}
