package me.ezra_home.retail_software_solution.organizations.business.stock_item_source.public

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.inventory.StockItemSource
import me.ezra_home.retail_software_solution.organizations.business.inventory.StockItemSourceRepository
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class StockItemSourceService(
  private val stockItemSourceRepository: StockItemSourceRepository
) {

  fun findSourceId(code: StockItemSource): UUID =
    stockItemSourceRepository.findByCode(code)?.id
      ?: throw RtsGenericException("Stock item source '${code}' not found.")
}
