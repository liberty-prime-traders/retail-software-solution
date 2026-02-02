package me.ezra_home.retail_software_solution.cross_tier.product.search.common

object ProductSearchValidator {

  private const val MAX_ARRAY_SIZE = 50

  fun validateArraySizes(
    categoryIds: Collection<*>,
    statusList: Collection<*>,
    tagIds: Collection<*>? = null
  ) {
    require(categoryIds.size <= MAX_ARRAY_SIZE) {
      "categoryIds exceeds maximum size of $MAX_ARRAY_SIZE"
    }
    require(statusList.size <= MAX_ARRAY_SIZE) {
      "statusList exceeds maximum size of $MAX_ARRAY_SIZE"
    }
    tagIds?.let {
      require(it.size <= MAX_ARRAY_SIZE) {
        "tagIds exceeds maximum size of $MAX_ARRAY_SIZE"
      }
    }
  }
}
