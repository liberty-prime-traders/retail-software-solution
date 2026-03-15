package me.ezra_home.retail_software_solution.cucumber.support

import com.fasterxml.jackson.databind.ObjectMapper
import io.cucumber.datatable.DataTable
import org.springframework.stereotype.Component

@Component
class DtoConverter(
  private val mapper: ObjectMapper,
  private val injectContext: InjectContext
) {

  companion object {
    const val NULL = "NULL"
    const val NONE = "NONE"
    const val DEFAULT = "DEFAULT"

    private val EMPTY_SENTINELS = setOf(NONE, DEFAULT)
  }

  fun <T> fromRow(row: Map<String, String>, clazz: Class<T>): T =
    mapper.convertValue(row.injectAndNullify(), clazz)

  fun <T> fromTable(dataTable: DataTable, clazz: Class<T>): List<T> =
    dataTable.asMaps().map { fromRow(it, clazz) }

  fun <T> fromJson(json: String, clazz: Class<T>): T {
    val injected = injectContext.inject(json)
    val effective = if (injected.trim().uppercase() in EMPTY_SENTINELS) "{}" else injected
    return mapper.readValue(effective, clazz)
  }

  private fun Map<String, String>.injectAndNullify(): Map<String, String?> =
    mapValues { (_, v) ->
      val injected = injectContext.inject(v)
      if (injected.equals(NULL, ignoreCase = true)) null else injected
    }
}
