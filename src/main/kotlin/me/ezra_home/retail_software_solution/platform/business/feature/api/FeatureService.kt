package me.ezra_home.retail_software_solution.platform.business.feature.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.feature.api.OrganizationFeatureService
import me.ezra_home.retail_software_solution.platform.business.feature.FeatureCache
import me.ezra_home.retail_software_solution.platform.business.feature.FeatureDto
import me.ezra_home.retail_software_solution.platform.business.organization.api.OrganizationService
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnPlatformSchema(readOnly = true)
class FeatureService(
    private val featureCache: FeatureCache,
    private val organizationService: OrganizationService,
    private val organizationFeatureService: OrganizationFeatureService
) {

    fun getAll(): Collection<FeatureDto> = featureCache.getAll()

    fun activate(organizationId: UUID, features: HashSet<Feature>) {
        val org = organizationService.getAllOrganizationDtos().find { it.id == organizationId }
            ?: throw RtsGenericException("Organization $organizationId not found")
        SessionContextProvider.initOrganization(org)
        organizationFeatureService.activate(features)
    }

    fun deactivate(organizationId: UUID, features: HashSet<Feature>) {
        val org = organizationService.getAllOrganizationDtos().find { it.id == organizationId }
            ?: throw RtsGenericException("Organization $organizationId not found")
        SessionContextProvider.initOrganization(org)
        organizationFeatureService.deactivate(features)
    }

    @TransactionalOnPlatformSchema
    fun updateDescription(feature: FeatureUpdateDto): FeatureDto {
        val existing = featureCache.getAll().find { it.feature == feature.feature }
            ?: throw RtsGenericException("Feature ${feature.feature} not found")
        val updated = feature.applyTo(existing)
        featureCache.update(updated)
        return updated
    }
}
