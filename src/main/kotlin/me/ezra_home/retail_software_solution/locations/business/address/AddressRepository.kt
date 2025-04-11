package me.ezra_home.retail_software_solution.locations.business.address

import me.ezra_home.retail_software_solution.locations.model.AddressEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AddressRepository: JpaRepository<AddressEntity, UUID>
