package me.ezra_home.retail_software_solution.organizations.business.unitgroup

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.org_profile.api.OrgDataSeeder
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(OrgDataSeeder.UNIT_GROUP)
@TransactionalOnOrganizationSchema
class UnitGroupSeeder(private val unitGroupCache: UnitGroupCache) : OrgDataSeeder {

    override fun seed() {
        val existingCodes = unitGroupCache.getAllUnitGroups().map { it.code }.toSet()
        val toInsert = SystemUnitGroup.entries
            .filter { it.code !in existingCodes }
            .map { UnitGroupEntity(code = it.code, name = it.groupName, description = it.description, systemDefined = true) }
        if (toInsert.isNotEmpty()) {
            unitGroupCache.saveAll(toInsert)
        }
    }
}
