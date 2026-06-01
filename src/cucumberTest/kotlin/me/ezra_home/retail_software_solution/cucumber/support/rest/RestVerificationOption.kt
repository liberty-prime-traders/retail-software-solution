package me.ezra_home.retail_software_solution.cucumber.support.rest

enum class RestVerificationOption {
  DEFAULT,
  ITEM_WITH,
  NO_ITEM_WITH;

  fun negated(): Boolean = this == NO_ITEM_WITH

  companion object {
    const val REGEX = "( item with | no item with |\\s)"

    fun fromToken(raw: String): RestVerificationOption = when (raw.trim()) {
      "item with" -> ITEM_WITH
      "no item with" -> NO_ITEM_WITH
      else -> DEFAULT
    }
  }
}

enum class OrderOption {
  DEFAULT,
  IN_ORDER;

  fun inOrder(): Boolean = this == IN_ORDER

  companion object {
    const val REGEX = "( in order|\\s)"

    fun fromToken(raw: String): OrderOption =
      if (raw.trim() == "in order") IN_ORDER else DEFAULT
  }
}

enum class ParameterStyle {
  QUERY,
  MATRIX;

  companion object {
    const val REGEX = "query|matrix"

    fun fromToken(raw: String): ParameterStyle =
      if (raw == "query") QUERY else MATRIX
  }
}
