package me.ezra_home.retail_software_solution.locations.business.sale_payment.search.filters

import me.ezra_home.retail_software_solution.locations.business.sale_payment.search.SalePaymentSearchAliases
import me.ezra_home.retail_software_solution.locations.business.sale_payment.search.SalePaymentSearchParameterNames
import me.ezra_home.retail_software_solution.util.queries.FilterStrategy
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext

class SaleReferenceNumbersFilterStrategy(private val saleReferenceNumbers: List<String>) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    if (saleReferenceNumbers.isNotEmpty()) {
      context.whereClauses.add("${SalePaymentSearchAliases.SALE}.reference_number = ANY(:${SalePaymentSearchParameterNames.SALE_REFERENCE_NUMBERS})")
      context.params[SalePaymentSearchParameterNames.SALE_REFERENCE_NUMBERS] = saleReferenceNumbers.toTypedArray()
    }
  }
}
