package me.ezra_home.retail_software_solution.organizations.business.locationadmin

import me.ezra_home.retail_software_solution.organizations.model.LocationAdminEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LocationAdminRepository: JpaRepository<LocationAdminEntity, UUID> {
    fun findByLocationId(locationId: UUID): List<LocationAdminEntity>
}
