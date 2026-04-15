package me.ezra_home.retail_software_solution.organizations.business.feature.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.feature.activators.FeatureActivatorRegistry
import me.ezra_home.retail_software_solution.organizations.business.feature.OrganizationFeatureCache
import me.ezra_home.retail_software_solution.organizations.business.feature.OrganizationFeatureDto
import me.ezra_home.retail_software_solution.organizations.business.feature.OrganizationFeatureMapper
import me.ezra_home.retail_software_solution.organizations.business.feature.OrganizationFeatureStatus
import me.ezra_home.retail_software_solution.platform.business.feature.api.Feature
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
@TransactionalOnOrganizationSchema
class OrganizationFeatureService(
    private val featureCache: OrganizationFeatureCache,
    private val activatorRegistry: FeatureActivatorRegistry,
    private val mapper: OrganizationFeatureMapper
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAll(): List<OrganizationFeatureResponseDto> = featureCache.getAll().map { mapper.toResponseDto(it) }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun isActive(feature: Feature): Boolean {
        return featureCache.getAll().any {
            it.feature == feature && it.status == OrganizationFeatureStatus.ACTIVE
        }
    }

    fun activate(features: HashSet<Feature>): List<OrganizationFeatureResponseDto> {
        val existingByCode = featureCache.getAll().associateBy { it.feature }
        val toSave = buildActivateDtos(features, existingByCode)
        toSave.forEach {
            activatorRegistry.getHandler(it.feature).forEach {
                handler -> handler.activate()
            }
        }
        if (toSave.isNotEmpty()) featureCache.saveAll(toSave)
        return featureCache.getAll().map { mapper.toResponseDto(it) }
    }

    fun deactivate(features: HashSet<Feature>): List<OrganizationFeatureResponseDto> {
        val userId = SessionContextProvider.getUserId()
        val now = OffsetDateTime.now()
        val existing = featureCache.getAll().associateBy { it.feature }

        val toSave = features.mapNotNull { feature ->
            val current = existing[feature] ?: return@mapNotNull null
            if (current.status == OrganizationFeatureStatus.INACTIVE) return@mapNotNull null
            current.copy(status = OrganizationFeatureStatus.INACTIVE, disabledOn = now, disabledBy = userId)
        }

        toSave.forEach {
            activatorRegistry.getHandler(it.feature).forEach { handler -> handler.deactivate() }
        }
        if (toSave.isNotEmpty()) featureCache.saveAll(toSave)
        return featureCache.getAll().map { mapper.toResponseDto(it) }
    }

    private fun buildActivateDtos(features: HashSet<Feature>, existingByCode: Map<Feature, OrganizationFeatureDto>): List<OrganizationFeatureDto> {
        val userId = SessionContextProvider.getUserId()
        val now = OffsetDateTime.now()
        return features.map { feature ->
            existingByCode[feature]
                ?.copy(status = OrganizationFeatureStatus.ACTIVE, enabledOn = now, enabledBy = userId, disabledOn = null, disabledBy = null)
                ?: OrganizationFeatureDto(feature = feature, status = OrganizationFeatureStatus.ACTIVE, enabledOn = now, enabledBy = userId)
        }
    }
}
