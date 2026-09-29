package me.ezra_home.retail_software_solution.util.queries

import java.time.OffsetDateTime

class DateRangeFilterStrategy(
  private val column: String,
  private val fromParam: String,
  private val fromValue: OffsetDateTime?,
  private val beforeParam: String,
  private val beforeValue: OffsetDateTime?
) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    fromValue?.let {
      context.whereClauses.add("$column >= :$fromParam")
      context.params[fromParam] = it
    }
    beforeValue?.let {
      context.whereClauses.add("$column < :$beforeParam")
      context.params[beforeParam] = it
    }
  }
}
