package me.ezra_home.retail_software_solution.platform.business.organization

import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.Objects
import java.util.Optional
import java.util.UUID

@Component
class OrganizationValidator(private val organizationCache: OrganizationCache) {

    fun validateNameOnSave(name: Optional<String>?, id: UUID? = null) {
        if (name == null || name.isEmpty || name.get().isBlank()) {
            throw RtsGenericException("An Organization must have a name")
        }
        val organizationWithMatchingName = organizationCache.getAllOrganizations().find {
            it.name.equals(name.get(), ignoreCase = true) && !Objects.equals(it.id, id)
        }
        if (organizationWithMatchingName != null) {
            throw RtsGenericException("An organization using the name '${name.get()}' already exists")
        }
    }
}
