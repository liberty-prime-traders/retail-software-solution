package me.ezra_home.retail_software_solution.configuration

import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import me.ezra_home.retail_software_solution.util.listeners.OrganizationReferenceNumberEntityListener
import me.ezra_home.retail_software_solution.util.listeners.LocationReferenceNumberEntityListener
import me.ezra_home.retail_software_solution.util.service.OrganizationReferenceNumberGeneratorService
import me.ezra_home.retail_software_solution.util.service.LocationReferenceNumberGeneratorService

@Configuration
class EntityListenerInjector(
    private val organizationReferenceNumberGeneratorService: OrganizationReferenceNumberGeneratorService,
    private val locationReferenceNumberGeneratorService: LocationReferenceNumberGeneratorService
) {
    @PostConstruct
    fun inject() {
        OrganizationReferenceNumberEntityListener.organizationReferenceNumberGeneratorService = organizationReferenceNumberGeneratorService
        LocationReferenceNumberEntityListener.locationReferenceNumberGeneratorService = locationReferenceNumberGeneratorService
    }
}

