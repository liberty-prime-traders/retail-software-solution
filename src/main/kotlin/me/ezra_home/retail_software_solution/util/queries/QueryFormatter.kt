package me.ezra_home.retail_software_solution.util.queries

import java.util.UUID

object QueryFormatter {

  fun formatQueryWithParameters(sqlQuery: SqlQuery, pageSize: Int, pageSizeParamName: String = "pageSize"): String {
    var formattedSql = sqlQuery.sql
    val params = sqlQuery.params.toMutableMap()
    params[pageSizeParamName] = pageSize

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
