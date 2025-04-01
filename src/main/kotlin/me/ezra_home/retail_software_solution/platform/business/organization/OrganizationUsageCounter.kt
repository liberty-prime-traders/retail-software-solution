package me.ezra_home.retail_software_solution.platform.business.organization


import me.ezra_home.retail_software_solution.platform.model.OrganizationEntity
import me.ezra_home.retail_software_solution.util.business.UsageCounter
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OrganizationUsageCounter(private val organizationCache: OrganizationCache) : UsageCounter<OrganizationEntity> {

    override fun incrementUsageCount(id: UUID?) {
        organizationCache.getAllOrganizations().find { it.id == id }?.let {
            it.usageCount.plus(1L)
            organizationCache.upsertOrganization(it)
        }
    }

    override fun decrementUsageCount(id: UUID?) {
        organizationCache.getAllOrganizations().find { it.id == id }?.let {
            it.usageCount.minus(1L)
            organizationCache.upsertOrganization(it)
        }
    }
}
