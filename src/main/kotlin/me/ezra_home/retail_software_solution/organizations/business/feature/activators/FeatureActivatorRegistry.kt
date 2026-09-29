package me.ezra_home.retail_software_solution.organizations.business.feature.activators

import me.ezra_home.retail_software_solution.platform.business.feature.api.Feature
import org.springframework.stereotype.Component

@Component
class FeatureActivatorRegistry(activators: List<FeatureActivator>) {
    private val handlers: Map<Feature, List<FeatureActivator>> =  activators.groupBy { it.feature }

    fun getHandler(feature: Feature): List<FeatureActivator> = handlers[feature] ?: emptyList()
}
