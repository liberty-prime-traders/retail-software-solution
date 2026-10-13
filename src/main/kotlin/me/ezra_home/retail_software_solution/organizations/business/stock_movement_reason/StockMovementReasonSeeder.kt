package me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.org_profile.api.OrgDataSeeder
import org.springframework.stereotype.Component

@Component
@TransactionalOnOrganizationSchema
class StockMovementReasonSeeder(
    private val stockMovementReasonCache: StockMovementReasonCache
) : OrgDataSeeder {

    override fun seed() {
        val existingReasonsByCode = stockMovementReasonCache.getAll()
            .filter { it.code != null }
            .associateBy { it.code }
        val missingReasonEntities = StockMovementReason.entries
            .filter { it.code !in existingReasonsByCode }
            .map {
                StockMovementReasonEntity(
                    code = it.code,
                    name = it.displayName,
                    description = it.description,
                    movementTypes = it.allowedMovementTypes,
                    systemDefined = true
                )
            }
        if (missingReasonEntities.isNotEmpty()) {
            stockMovementReasonCache.saveAll(missingReasonEntities)
        }

        StockMovementReason.entries.forEach { systemReason ->
            val existingReason = existingReasonsByCode[systemReason.code] ?: return@forEach
            if (existingReason.movementTypes != systemReason.allowedMovementTypes) {
                stockMovementReasonCache.save(existingReason.copy(movementTypes = systemReason.allowedMovementTypes))
            }
        }
    }
}
