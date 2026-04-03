package me.ezra_home.retail_software_solution.cucumber.support.context

sealed interface ContextKey {
  val key: String
}

enum class PersistentKey(override val key: String) : ContextKey {
  ORGANIZATION("organization")
}

enum class TransientKey(override val key: String) : ContextKey {
  PRODUCT("product"),
  CATEGORY("category"),
  PRODUCT_GROUP("productGroup"),
  UNIT_GROUP("unitGroup"),
  UNIT_VALUE("unitValue"),
  LOCATION("location")
}
