package me.ezra_home.retail_software_solution.organizations.business.address

import me.ezra_home.retail_software_solution.organizations.business.address.AddressEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AddressRepository: JpaRepository<AddressEntity, UUID>
