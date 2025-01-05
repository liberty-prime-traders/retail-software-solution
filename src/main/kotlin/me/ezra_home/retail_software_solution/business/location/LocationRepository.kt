package me.ezra_home.retail_software_solution.business.location

import me.ezra_home.retail_software_solution.model.entity.LocationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LocationRepository : JpaRepository<LocationEntity, UUID> {

    fun findByOrganizationId(organizationId: UUID): Collection<LocationEntity>
}
