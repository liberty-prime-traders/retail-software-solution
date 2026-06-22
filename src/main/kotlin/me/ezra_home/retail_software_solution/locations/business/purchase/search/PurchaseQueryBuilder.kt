package me.ezra_home.retail_software_solution.locations.business.purchase.search

import me.ezra_home.retail_software_solution.locations.business.purchase.search.PurchaseSearchParams.Columns
import me.ezra_home.retail_software_solution.locations.business.purchase.search.PurchaseSearchParams.Params
import me.ezra_home.retail_software_solution.locations.business.purchase.search.strategies.CreatedByFilterStrategy
import me.ezra_home.retail_software_solution.locations.business.purchase.search.strategies.DateRangeFilterStrategy
import me.ezra_home.retail_software_solution.locations.business.purchase.search.strategies.OrderedByFilterStrategy
import me.ezra_home.retail_software_solution.locations.business.purchase.search.strategies.PurchaseStatusFilterStrategy
import me.ezra_home.retail_software_solution.locations.business.purchase.search.strategies.SupplierFilterStrategy
import me.ezra_home.retail_software_solution.util.model.TableNames
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext
import me.ezra_home.retail_software_solution.util.queries.QueryMetadata
import me.ezra_home.retail_software_solution.util.queries.SqlQuery

object PurchaseQueryBuilder {

  fun build(request: PurchaseSearchRequest): SqlQuery {
    val context = QueryBuilderContext()
    context.whereClauses.add("1=1")

    SupplierFilterStrategy(request.supplierIds).apply(context)
    OrderedByFilterStrategy(request.orderedByIds).apply(context)
    CreatedByFilterStrategy(request.createdByIds).apply(context)
    PurchaseStatusFilterStrategy(request.statuses).apply(context)
    DateRangeFilterStrategy(Columns.DATE_ORDERED, Params.DATE_ORDERED_FROM, Params.DATE_ORDERED_TO, request.dateOrderedFrom, request.dateOrderedTo).apply(context)
    DateRangeFilterStrategy(Columns.CREATED_ON, Params.CREATED_ON_FROM, Params.CREATED_ON_TO, request.createdOnFrom, request.createdOnTo).apply(context)

    context.params[Params.LIMIT] = request.limit.coerceIn(1, 1000)

    val sql = """
      SELECT p.*
      FROM ${TableNames.PURCHASE} p
      WHERE ${context.whereClauses.joinToString(" AND ")}
      ORDER BY p.${request.sortBy.columnName} DESC
      LIMIT :${Params.LIMIT}
    """.trimIndent()

    return SqlQuery(sql, context.params, QueryMetadata(queryName = "purchase_search"))
  }
}
