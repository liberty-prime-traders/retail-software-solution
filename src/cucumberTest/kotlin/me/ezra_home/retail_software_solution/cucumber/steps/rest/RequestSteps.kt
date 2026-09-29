package me.ezra_home.retail_software_solution.cucumber.steps.rest

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.When
import me.ezra_home.retail_software_solution.cucumber.support.ApiClient
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.rest.ParameterStyle
import org.springframework.http.HttpMethod

class RequestSteps(
  private val apiClient: ApiClient,
  private val injectContext: InjectContext
) {

  @When("I {httpMethod} to/from {path}")
  fun openRequest(method: HttpMethod, path: String) {
    apiClient.exchange(method, injectContext.inject(path))
  }

  @When("I {httpMethod} to/from {path} with payload:")
  fun openRequestWithPayload(method: HttpMethod, path: String, payload: String) {
    apiClient.exchange(method, injectContext.inject(path), body = injectContext.inject(payload))
  }

  @When("I {httpMethod} to/from {path} with {parameterStyle} parameters:")
  fun openRequestWithParameters(method: HttpMethod, path: String, style: ParameterStyle, parameters: DataTable) {
    val params = parametersFrom(parameters)
    val resolvedPath = injectContext.inject(path)
    when (style) {
      ParameterStyle.QUERY -> apiClient.exchange(method, resolvedPath, queryParams = params)
      ParameterStyle.MATRIX -> apiClient.exchange(method, appendMatrixParameters(resolvedPath, params))
    }
  }

  private fun parametersFrom(table: DataTable): Map<String, Any> =
    table.asLists().associate { row ->
      val key = row[0] ?: error("Missing parameter name in row $row")
      val value = injectContext.inject(row.getOrNull(1).orEmpty())
      key to value
    }

  private fun appendMatrixParameters(path: String, params: Map<String, Any>): String {
    if (params.isEmpty()) return path
    val matrix = params.entries.joinToString(separator = ";", prefix = ";") { (k, v) -> "$k=$v" }
    return path + matrix
  }
}
