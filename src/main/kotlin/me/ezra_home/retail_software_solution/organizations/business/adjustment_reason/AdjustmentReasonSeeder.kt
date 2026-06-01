package me.ezra_home.retail_software_solution.organizations.business.adjustment_reason

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.SystemAdjustmentReason
import me.ezra_home.retail_software_solution.organizations.business.org_profile.api.OrgDataSeeder
import org.springframework.stereotype.Component

@Component
@TransactionalOnOrganizationSchema
class AdjustmentReasonSeeder(
    private val adjustmentReasonCache: AdjustmentReasonCache
) : OrgDataSeeder {

    override fun seed() {
        val existingCodes = adjustmentReasonCache.getAll().mapNotNull { it.code }.toSet()
        val toInsert = SystemAdjustmentReason.entries
            .filter { it.code !in existingCodes }
            .map {
                AdjustmentReasonEntity(
                    name = it.displayName,
                    code = it.code,
                    direction = it.direction,
                    systemDefined = true
                )
            }
        if (toInsert.isNotEmpty()) {
            adjustmentReasonCache.saveAll(toInsert)
        }
    }
}
