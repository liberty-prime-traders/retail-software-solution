package me.ezra_home.retail_software_solution.organizations.business.unitvalue

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.org_profile.api.OrgDataSeeder
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupService
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(OrgDataSeeder.UNIT_VALUE)
@TransactionalOnOrganizationSchema
class UnitValueSeeder(
    private val unitValueCache: UnitValueCache,
    private val unitGroupService: UnitGroupService
) : OrgDataSeeder {

    override fun seed() {
        val unitGroups = unitGroupService.getAllUnitGroupDtos().associateBy { it.code }
        val existingValues = unitValueCache.getAllUnitValues().associateBy { it.code }.toMutableMap()
        val existingCodes = existingValues.keys

        SystemUnitValue.entries
            .filter { it.code !in existingCodes }
            .forEach { unitValue ->
                val groupId = unitGroups[unitValue.group.code]?.id ?: return@forEach
                val baseUnitId = unitValue.baseUnit?.let { existingValues[it.code]?.id }
                val entity = UnitValueEntity(
                    name = unitValue.unitName,
                    code = unitValue.code,
                    unitGroupId = groupId,
                    baseUnit = baseUnitId,
                    conversionFactor = unitValue.conversionFactor?.toBigDecimal(),
                    systemDefined = true
                )
                existingValues[unitValue.code] = unitValueCache.save(entity)
            }
    }
}
