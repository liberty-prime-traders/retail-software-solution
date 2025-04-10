package me.ezra_home.retail_software_solution.platform.business.location

import me.ezra_home.retail_software_solution.platform.model.LocationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LocationRepository : JpaRepository<LocationEntity, UUID> {

    fun findByOrganizationId(organizationId: UUID): Collection<LocationEntity>
}
