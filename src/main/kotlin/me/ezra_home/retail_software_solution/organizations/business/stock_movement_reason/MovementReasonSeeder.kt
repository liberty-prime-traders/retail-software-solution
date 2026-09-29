package me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.org_profile.api.OrgDataSeeder
import org.springframework.stereotype.Component

@Component
@TransactionalOnOrganizationSchema
class MovementReasonSeeder(
    private val stockMovementReasonCache: StockMovementReasonCache
) : OrgDataSeeder {

    override fun seed() {
        val existingCodes = stockMovementReasonCache.getAll().map { it.code }.toSet()
        val toInsert = MovementReason.entries
            .filter { it.code !in existingCodes }
            .map {
                StockMovementReasonEntity(
                    code = it.code,
                    name = it.displayName,
                    description = it.description,
                    systemDefined = true
                )
            }
        if (toInsert.isNotEmpty()) {
            stockMovementReasonCache.saveAll(toInsert)
        }
    }
}
