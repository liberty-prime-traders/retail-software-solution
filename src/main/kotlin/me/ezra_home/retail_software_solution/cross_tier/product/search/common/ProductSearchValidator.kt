package me.ezra_home.retail_software_solution.cross_tier.product.search.common

import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException

object ProductSearchValidator {

  private const val MAX_ARRAY_SIZE = 50

  fun validateArraySizes(
    categoryIds: Collection<*>,
    statusList: Collection<*>,
    tagIds: Collection<*>? = null
  ) {
    if (categoryIds.size > MAX_ARRAY_SIZE) {
      throw RtsGenericException("categoryIds exceeds maximum size of $MAX_ARRAY_SIZE")
    }
    if (statusList.size > MAX_ARRAY_SIZE) {
      throw RtsGenericException("statusList exceeds maximum size of $MAX_ARRAY_SIZE")
    }
    tagIds?.let {
      if (it.size > MAX_ARRAY_SIZE) {
        throw RtsGenericException("tagIds exceeds maximum size of $MAX_ARRAY_SIZE")
      }
    }
  }
}
