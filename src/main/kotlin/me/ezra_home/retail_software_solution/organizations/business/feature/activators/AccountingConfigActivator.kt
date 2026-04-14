package me.ezra_home.retail_software_solution.organizations.business.feature.activators

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.OrgAccountingConfigService
import me.ezra_home.retail_software_solution.platform.business.feature.api.Feature
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema
class AccountingConfigActivator(private val configService: OrgAccountingConfigService) : FeatureActivator {

    override val feature = Feature.CHART_OF_ACCOUNTS

    override fun onActivate() = configService.initialize()
}
