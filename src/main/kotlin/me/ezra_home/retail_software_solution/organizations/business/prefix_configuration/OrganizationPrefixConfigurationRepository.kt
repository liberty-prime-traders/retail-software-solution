package me.ezra_home.retail_software_solution.organizations.business.prefix_configuration

import me.ezra_home.retail_software_solution.organizations.model.OrganizationPrefixConfigurationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OrganizationPrefixConfigurationRepository : JpaRepository<OrganizationPrefixConfigurationEntity, UUID> {
    fun findByTableRegistryId(tableRegistryId: UUID): Collection<OrganizationPrefixConfigurationEntity>
}
