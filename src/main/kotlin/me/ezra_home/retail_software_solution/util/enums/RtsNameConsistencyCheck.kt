package me.ezra_home.retail_software_solution.util.enums

import me.ezra_home.retail_software_solution.platform.business.startup_checks.api.StartupCheck
import org.springframework.stereotype.Component

@Component
class RtsNameConsistencyCheck : StartupCheck {

    override val name = "rts-role-and-permission-name-consistency"

    override fun check() {
        requireNamesMatch("RtsRoleNames", RtsRoleNames::class.java, RtsRole.entries.map { it.name })
        requireNamesMatch("RtsPermissionNames", RtsPermissionNames::class.java, RtsPermission.entries.map { it.name })
    }

    private fun requireNamesMatch(label: String, constantsClass: Class<*>, enumNames: List<String>) {
        val declaredNames = constantsClass.fields
            .filter { it.name != "INSTANCE" }
            .map { it.get(null) as String }
            .toSet()
        val enumNameSet = enumNames.toSet()
        if (declaredNames != enumNameSet) {
            throw IllegalStateException(
                "$label is out of sync with its enum - missing: ${enumNameSet - declaredNames}, extra: ${declaredNames - enumNameSet}"
            )
        }
    }
}
