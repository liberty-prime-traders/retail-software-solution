package me.ezra_home.retail_software_solution.configuration.injectors

import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import me.ezra_home.retail_software_solution.util.listeners.OrganizationReferenceNumberEntityListener
import me.ezra_home.retail_software_solution.util.service.OrganizationReferenceNumberGeneratorService

@Configuration
class EntityListenerInjector(
    private val organizationReferenceNumberGeneratorService: OrganizationReferenceNumberGeneratorService,
) {
    @PostConstruct
    fun inject() {
        OrganizationReferenceNumberEntityListener.organizationReferenceNumberGeneratorService = organizationReferenceNumberGeneratorService
    }
}
