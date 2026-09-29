package me.ezra_home.retail_software_solution.util.queries

// `values` must already be a concretely-typed array (Array<UUID>, Array<String>, ...), never Array<Any> -
// Hibernate infers the Postgres array element type from the array's runtime component type, and an erased
// Object[] fails with "op ANY/ALL (array) requires array on right side".
class AnyListFilterStrategy(
  private val columnExpression: String,
  private val paramName: String,
  private val values: Array<out Any>
) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    if (values.isNotEmpty()) {
      context.whereClauses.add("$columnExpression = ANY(:$paramName)")
      context.params[paramName] = values
    }
  }
}
