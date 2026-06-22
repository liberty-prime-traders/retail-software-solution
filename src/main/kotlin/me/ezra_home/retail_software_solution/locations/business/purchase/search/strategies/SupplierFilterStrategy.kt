package me.ezra_home.retail_software_solution.locations.business.purchase.search.strategies

import me.ezra_home.retail_software_solution.locations.business.purchase.search.PurchaseSearchParams.Columns
import me.ezra_home.retail_software_solution.locations.business.purchase.search.PurchaseSearchParams.Params
import me.ezra_home.retail_software_solution.util.queries.FilterStrategy
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext
import java.util.UUID

class SupplierFilterStrategy(private val supplierIds: List<UUID>?) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    if (!supplierIds.isNullOrEmpty()) {
      context.whereClauses.add("p.${Columns.SUPPLIER_ID} = ANY(:${Params.SUPPLIER_IDS})")
      context.params[Params.SUPPLIER_IDS] = supplierIds.toTypedArray()
    }
  }
}
