package me.ezra_home.retail_software_solution.organizations.business.product.search

object ProductSearchValidator {

  private const val MAX_ARRAY_SIZE = 50

  fun validateArraySizes(params: ProductSearchParameters) {
    require(params.categoryIds.size <= MAX_ARRAY_SIZE) {
      "categoryIds exceeds maximum size of $MAX_ARRAY_SIZE"
    }
    require(params.tagsIds.size <= MAX_ARRAY_SIZE) {
      "tagIds exceeds maximum size of $MAX_ARRAY_SIZE"
    }
    require(params.statusList.size <= MAX_ARRAY_SIZE) {
      "statusList exceeds maximum size of $MAX_ARRAY_SIZE"
    }
  }

}
