package me.ezra_home.retail_software_solution.util.queries

interface FilterStrategy {
  fun apply(context: QueryBuilderContext)
}
