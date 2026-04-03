package me.ezra_home.retail_software_solution.cucumber.support

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.cucumber.datatable.DataTable
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import org.springframework.stereotype.Component

@Component
class DtoConverter(
  private val mapper: ObjectMapper,
  private val injectContext: InjectContext
) {

  fun <T> fromRow(row: Map<String, String>, clazz: Class<T>): T =
    mapper.convertValue(row.injectAndNullify(), clazz)

  fun <T : Any> fromRow(row: Map<String, String>, defaults: T, clazz: Class<T>): T {
    val node = mapper.valueToTree<ObjectNode>(defaults)
    row.injectAndNullify()
      .filterValues { it != null }
      .forEach { (key, value) -> node.put(key, value!!) }
    return mapper.treeToValue(node, clazz)
  }

  fun <T> fromTable(dataTable: DataTable, clazz: Class<T>): List<T> =
    dataTable.asMaps().map { fromRow(it, clazz) }

  private fun Map<String, String>.injectAndNullify(): Map<String, String?> =
    mapValues { (_, v) -> injectContext.inject(v).ifBlank { null } }
}
