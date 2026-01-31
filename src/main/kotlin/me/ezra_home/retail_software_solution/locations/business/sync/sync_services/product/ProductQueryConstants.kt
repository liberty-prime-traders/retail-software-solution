package me.ezra_home.retail_software_solution.locations.business.sync.sync_services.product

object ProductQueryConstants {
  object Tables {
    const val MAIN = "product"
    const val AUDIT = "product_aud"
    const val GROUP = "product_group"
  }

  object Aliases {
    const val PRODUCT_TABLE = "p"
    const val PRODUCT_AUDIT_TABLE = "a"
    const val PRODUCT_GROUP_TABLE = "pg"
  }

  object Columns {
    const val ID = "id"
    const val NAME = "name"
    const val PRODUCT_GROUP_NAME = "product_group_name"
    const val STATUS = "status"
    const val REFERENCE_NUMBER = "reference_number"
    const val BASE_UNIT_ID = "base_unit_id"
    const val PRODUCT_GROUP_ID = "product_group_id"
    const val CATEGORY_ID = "category_id"
    const val REVISION = "revision"
  }
}
