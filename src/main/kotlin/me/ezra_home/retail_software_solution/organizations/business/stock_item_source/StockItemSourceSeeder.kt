package me.ezra_home.retail_software_solution.organizations.business.stock_item_source

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.org_profile.api.OrgDataSeeder
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import org.springframework.stereotype.Component

@Component
@TransactionalOnOrganizationSchema
class StockItemSourceSeeder(
    private val stockItemSourceRepository: StockItemSourceRepository
) : OrgDataSeeder {

    override fun seed() {
        val existingCodes = stockItemSourceRepository.findAll().map { it.code }.toSet()
        val toInsert = StockItemSource.entries
            .filter { it !in existingCodes }
            .map {
                StockItemSourceEntity(
                    code = it,
                    name = it.displayName,
                    description = it.description,
                    systemDefined = true
                )
            }
        if (toInsert.isNotEmpty()) {
            stockItemSourceRepository.saveAll(toInsert)
        }
    }
}
