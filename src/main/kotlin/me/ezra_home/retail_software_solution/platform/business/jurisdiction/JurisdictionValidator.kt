package me.ezra_home.retail_software_solution.platform.business.jurisdiction

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
        StringUtils.getValueOrException(name, "Name must not be blank")
    }

    fun validateTypeExists(jurisdictionTypeId: UUID) {
        if (jurisdictionTypeService.getAll().none { it.id == jurisdictionTypeId })
            throw RtsGenericException("Jurisdiction type not found")
    }

    fun validateParent(parentJurisdictionId: UUID?, selfId: UUID? = null) {
        if (parentJurisdictionId == null) return
        if (parentJurisdictionId == selfId)
            throw RtsGenericException("A jurisdiction cannot be its own parent")
        if (jurisdictionCache.getAll().none { it.id == parentJurisdictionId })
            throw RtsGenericException("Parent jurisdiction not found")
    }
}
