package me.ezra_home.retail_software_solution.organizations.business.feature.activators

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.feature.OrganizationFeatureCache
import me.ezra_home.retail_software_solution.organizations.business.feature.OrganizationFeatureStatus
import me.ezra_home.retail_software_solution.platform.business.feature.api.Feature
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class TaxConfigurationActivator(
    private val featureCache: OrganizationFeatureCache,
) : FeatureActivator {

    override val feature = Feature.TAX_CONFIGURATION

    override fun getUnmetPrerequisites(): Set<String> {
        val hasAccounting = featureCache.getAll().any {
            it.feature == Feature.CHART_OF_ACCOUNTS && it.status == OrganizationFeatureStatus.ACTIVE
        }
        return if (hasAccounting)
            emptySet()
        else
            setOf("'Tax Configuration' cannot be activated unless 'Chart of Accounts' is active")
    }
}
