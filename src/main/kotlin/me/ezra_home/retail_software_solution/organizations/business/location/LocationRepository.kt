package me.ezra_home.retail_software_solution.organizations.business.location

import me.ezra_home.retail_software_solution.organizations.model.LocationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
internal interface LocationRepository : JpaRepository<LocationEntity, UUID>
