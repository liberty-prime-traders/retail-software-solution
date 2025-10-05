package me.ezra_home.retail_software_solution.locations.business.prefix_configuration

import me.ezra_home.retail_software_solution.locations.model.LocationPrefixConfigurationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LocationPrefixConfigurationRepository : JpaRepository<LocationPrefixConfigurationEntity, UUID> {
    fun findByTableRegistryId(tableRegistryId: UUID): Collection<LocationPrefixConfigurationEntity>
}
