package me.ezra_home.retail_software_solution.cucumber.support

import com.fasterxml.jackson.databind.ObjectMapper
import io.cucumber.datatable.DataTable
import org.springframework.stereotype.Component

@Component
class DtoConverter(
  private val mapper: ObjectMapper,
  private val injectContext: InjectContext
) {

  fun <T> fromRow(row: Map<String, String>, clazz: Class<T>): T =
    mapper.convertValue(row.injectAndNullify(), clazz)

  fun <T> fromTable(dataTable: DataTable, clazz: Class<T>): List<T> =
    dataTable.asMaps().map { fromRow(it, clazz) }

  private fun Map<String, String>.injectAndNullify(): Map<String, String?> =
    mapValues { (_, v) -> injectContext.inject(v).ifBlank { null } }
}
