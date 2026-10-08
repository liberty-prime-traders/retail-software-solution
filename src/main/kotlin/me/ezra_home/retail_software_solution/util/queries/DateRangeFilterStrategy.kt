package me.ezra_home.retail_software_solution.util.queries

// T is the Java type bound to the column: OffsetDateTime for timestamptz, LocalDate for date.
class DateRangeFilterStrategy<T : Any>(
  private val column: String,
  private val fromParam: String,
  private val fromValue: T?,
  private val beforeParam: String,
  private val beforeValue: T?
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
