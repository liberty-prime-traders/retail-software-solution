package me.ezra_home.retail_software_solution.cross_tier.product.search.common

internal object Aliases {
  object ColumnNames {

    object CrossTierProduct {
      const val TABLE_ALIAS = "p"
      const val ID = "id"
      const val STATUS = "status"
      const val CATEGORY_ID = "category_id"
      const val PRODUCT_GROUP_NAME = "product_group_name"
      const val SEARCH_VECTOR = "search_vector"
      const val PRODUCT_NAME = "name"
      const val DESCRIPTION = "description"
      const val REFERENCE_NUMBER = "reference_number"
    }

    object OrganizationProduct {
      const val PRODUCT_GROUP_ID = "product_group_id"
    }

    object ProductGroup {
      const val TABLE_ALIAS = "pg"
      const val ID = "id"
    }

    object ProductTag {
      const val TABLE_ALIAS = "pt"
      const val TAG_ID = "tag_id"
      const val PRODUCT_ID = "product_id"
      const val END_ON = "end_on"
    }
  }
}
