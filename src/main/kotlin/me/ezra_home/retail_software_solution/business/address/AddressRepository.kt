package me.ezra_home.retail_software_solution.business.address

import me.ezra_home.retail_software_solution.model.entity.AddressEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AddressRepository: JpaRepository<AddressEntity, UUID>
