package me.ezra_home.retail_software_solution.platform.business.locationadmin

import me.ezra_home.retail_software_solution.platform.model.LocationAdminEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LocationAdminRepository: JpaRepository<LocationAdminEntity, UUID> {
    fun findByLocationId(locationId: UUID): List<LocationAdminEntity>
}
