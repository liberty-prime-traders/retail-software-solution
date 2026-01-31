package me.ezra_home.retail_software_solution.util.queries

data class SqlQuery(
  val sql: String,
  val params: Map<String, Any>,
  val metadata: QueryMetadata
)
