package me.ezra_home.retail_software_solution.organizations.business.product.search

import me.ezra_home.retail_software_solution.organizations.business.product.search.query_builder.ParameterNames
import java.util.UUID

object ProductSearchQueryFormatter {

  fun formatQueryWithParameters(sqlQuery: ProductSearchUtilityTypes.SqlQuery, pageSize: Int): String {
    var formattedSql = sqlQuery.sql
    val params = sqlQuery.params.toMutableMap()
    params[ParameterNames.PAGE_SIZE] = pageSize

    // Sort parameters by length (longest first) to avoid partial replacements
    val sortedParams = params.entries.sortedByDescending { it.key.length }

    sortedParams.forEach { (key, value) ->
      val placeholder = ":$key"
      val replacement = formatParameterValue(value)
      formattedSql = formattedSql.replace(placeholder, replacement)
    }

    return formattedSql
  }

  private fun formatParameterValue(value: Any): String {
    return when (value) {
      is String -> "'$value'"
      is UUID -> "'$value'"
      is Array<*> -> {
        val elements = value.joinToString(", ") { element ->
          when (element) {
            is UUID -> "'$element'"
            is String -> "'$element'"
            else -> element.toString()
          }
        }
        "ARRAY[$elements]"
      }
      is Collection<*> -> {
        val elements = value.joinToString(", ") { element ->
          when (element) {
            is UUID -> "'$element'"
            is String -> "'$element'"
            else -> element.toString()
          }
        }
        "ARRAY[$elements]"
      }
      is Number -> value.toString()
      is Boolean -> value.toString()
      else -> value.toString()
    }
  }
}
