package me.ezra_home.retail_software_solution.util.queries

internal interface FilterStrategy {
  fun apply(context: QueryBuilderContext)
}
