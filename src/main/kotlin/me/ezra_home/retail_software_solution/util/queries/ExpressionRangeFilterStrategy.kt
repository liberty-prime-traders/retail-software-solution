package me.ezra_home.retail_software_solution.util.queries

import java.math.BigDecimal

class ExpressionRangeFilterStrategy(
  private val expression: String,
  private val minParam: String,
  private val minValue: BigDecimal?,
  private val maxParam: String,
  private val maxValue: BigDecimal?
) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    minValue?.let {
      context.whereClauses.add("$expression >= :$minParam")
      context.params[minParam] = it
    }
    maxValue?.let {
      context.whereClauses.add("$expression <= :$maxParam")
      context.params[maxParam] = it
    }
  }
}
