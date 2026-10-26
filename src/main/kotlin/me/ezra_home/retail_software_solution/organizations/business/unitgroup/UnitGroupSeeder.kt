package me.ezra_home.retail_software_solution.organizations.business.unitgroup

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.lock.api.OrgEntityAdvisoryLock
import me.ezra_home.retail_software_solution.organizations.business.org_profile.api.OrgDataSeeder
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.SystemUnitGroup
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(OrgDataSeeder.UNIT_GROUP)
@TransactionalOnOrganizationSchema
class UnitGroupSeeder(
    private val unitGroupCache: UnitGroupCache,
    private val orgEntityAdvisoryLock: OrgEntityAdvisoryLock
) : OrgDataSeeder {

    override fun seed() {
        orgEntityAdvisoryLock.acquire(LockNamespaces.UNIT_GROUP_NAME, UnitGroupNameRules.LOCK_KEY)
        val existingUnitGroupDtos = unitGroupCache.getAllUnitGroups()
        val existingCodes = existingUnitGroupDtos.map { it.code }.toSet()
        val missingSystemUnitGroups = SystemUnitGroup.entries.filter { it.code !in existingCodes }
        UnitGroupNameRules.requireAvailable(existingUnitGroupDtos, missingSystemUnitGroups.map { it.groupName })
        if (missingSystemUnitGroups.isNotEmpty()) {
            unitGroupCache.saveAll(
                missingSystemUnitGroups.map {
                    UnitGroupEntity(code = it.code, name = it.groupName, description = it.description, systemDefined = true)
                }
            )
        }
    }
}
