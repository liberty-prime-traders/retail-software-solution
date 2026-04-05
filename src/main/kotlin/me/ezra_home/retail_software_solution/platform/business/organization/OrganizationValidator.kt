package me.ezra_home.retail_software_solution.platform.business.organization

import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.Objects
import java.util.Optional
import java.util.UUID

@Component
class OrganizationValidator(private val organizationCache: OrganizationCache) {

    fun validateNameOnSave(name: String, id: UUID? = null) {
        organizationCache.getAllOrganizations()
            .find { StringUtils.isEquivalent(it.name, name) && !Objects.equals(it.id, id) }
            ?.let { throw RtsGenericException("An organization using the name '$name' already exists") }
    }

    fun validateNameOnSave(optionalName: Optional<String>?, id: UUID? = null) {
        optionalName?.let {
            val name = StringUtils.getValueOrException(it, "Organization name cannot be empty")
            validateNameOnSave(name, id)
        }
    }
}
