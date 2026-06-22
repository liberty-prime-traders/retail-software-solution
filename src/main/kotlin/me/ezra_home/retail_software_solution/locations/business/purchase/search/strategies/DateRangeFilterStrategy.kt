package me.ezra_home.retail_software_solution.locations.business.purchase.search.strategies

import me.ezra_home.retail_software_solution.util.queries.FilterStrategy
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext
import java.time.OffsetDateTime

class DateRangeFilterStrategy(
  private val column: String,
  private val fromParamName: String,
  private val toParamName: String,
  private val from: OffsetDateTime?,
  private val to: OffsetDateTime?
) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    from?.let {
      context.whereClauses.add("p.$column >= :$fromParamName")
      context.params[fromParamName] = it
    }
    to?.let {
      context.whereClauses.add("p.$column <= :$toParamName")
      context.params[toParamName] = it
    }
  }
}
