package me.ezra_home.retail_software_solution.locations.business.location_user

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LocationUserRepository : JpaRepository<LocationUserEntity, UUID>
