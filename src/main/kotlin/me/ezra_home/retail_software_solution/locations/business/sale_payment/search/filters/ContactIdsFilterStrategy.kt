package me.ezra_home.retail_software_solution.locations.business.sale_payment.search.filters

import me.ezra_home.retail_software_solution.locations.business.sale_payment.search.SalePaymentSearchAliases
import me.ezra_home.retail_software_solution.locations.business.sale_payment.search.SalePaymentSearchParameterNames
import me.ezra_home.retail_software_solution.util.queries.FilterStrategy
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext
import java.util.UUID

class ContactIdsFilterStrategy(private val contactIds: List<UUID>) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    if (contactIds.isNotEmpty()) {
      context.whereClauses.add("${SalePaymentSearchAliases.SALE}.contact_id = ANY(:${SalePaymentSearchParameterNames.CONTACT_IDS})")
      context.params[SalePaymentSearchParameterNames.CONTACT_IDS] = contactIds.toTypedArray()
    }
  }
}
