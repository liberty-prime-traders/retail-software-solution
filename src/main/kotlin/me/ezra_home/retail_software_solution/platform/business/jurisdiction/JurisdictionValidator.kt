package me.ezra_home.retail_software_solution.platform.business.jurisdiction

import me.ezra_home.retail_software_solution.platform.business.jurisdiction.api.JurisdictionDto
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.api.JurisdictionTypeService
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class JurisdictionValidator(
    private val jurisdictionCache: JurisdictionCache,
    private val jurisdictionTypeService: JurisdictionTypeService
) {

    fun validateName(name: String) {
        StringUtils.requireHasValue(name, "Name must not be blank")
    }

    fun validateTypeExists(jurisdictionTypeId: UUID) {
        if (jurisdictionTypeService.getAll().none { it.id == jurisdictionTypeId })
            throw RtsGenericException("Jurisdiction type not found")
    }

    fun validateParent(parentJurisdictionId: UUID?, selfId: UUID? = null) {
        if (parentJurisdictionId == null) return
        if (parentJurisdictionId == selfId)
            throw RtsGenericException("A jurisdiction cannot be its own parent")
        val allJurisdictions = jurisdictionCache.getAll()
        if (allJurisdictions.none { it.id == parentJurisdictionId })
            throw RtsGenericException("Parent jurisdiction not found")
        if (selfId != null) {
            blockCircularAncestry(parentJurisdictionId, selfId, allJurisdictions)
        }
    }

    private fun blockCircularAncestry(
        potentialParentId: UUID,
        selfId: UUID,
        allJurisdictions: Collection<JurisdictionDto>
    ) {
        var currentParentId: UUID? = potentialParentId
        while (currentParentId != null) {
            if (currentParentId == selfId) {
                val parentName = allJurisdictions.firstOrNull { it.id == potentialParentId }?.name
                val childName = allJurisdictions.firstOrNull { it.id == currentParentId }?.name
                throw RtsGenericException("Circular hierarchy detected: $parentName cannot be set as parent of $childName because it is a descendant of $childName")
            }
            currentParentId = allJurisdictions.firstOrNull { it.id == currentParentId }?.parentJurisdictionId
        }
    }
}
