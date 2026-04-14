package me.ezra_home.retail_software_solution.organizations.business.feature.activators

import me.ezra_home.retail_software_solution.platform.business.feature.api.Feature
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException

interface FeatureActivator {
    val feature: Feature

    fun getUnmetPrerequisites(): Set<String> { return emptySet() }
    fun deactivate() {}
    fun onActivate() {}

    fun activate() {
        val unmetPrerequisites = getUnmetPrerequisites()
        if (unmetPrerequisites.isNotEmpty()) {
            throw RtsGenericException("Cannot activate feature $feature", unmetPrerequisites)
        }
        onActivate()
    }
}
