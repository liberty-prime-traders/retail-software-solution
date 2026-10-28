package me.ezra_home.retail_software_solution.organizations.business.unitgroup

import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupDto
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.util.UUID

object UnitGroupNameRules {

    const val LOCK_KEY = "names"

    fun requireAvailable(existingUnitGroupDtos: Collection<UnitGroupDto>, names: List<String>, excludedUnitGroupId: UUID? = null) {
        names.forEachIndexed { index, name ->
            val takenByEarlierName = names.take(index).any { StringUtils.isEquivalentOnAlphanumerics(it, name) }
            val takenByExistingGroup = existingUnitGroupDtos.any {
                it.id != excludedUnitGroupId && StringUtils.isEquivalentOnAlphanumerics(it.name, name)
            }
            if (takenByEarlierName || takenByExistingGroup) {
                throw RtsGenericException("An UnitGroup using the name '$name' already exists")
            }
        }
    }
}
