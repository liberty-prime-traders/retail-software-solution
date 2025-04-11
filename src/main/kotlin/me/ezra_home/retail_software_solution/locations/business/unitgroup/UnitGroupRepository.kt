package me.ezra_home.retail_software_solution.locations.business.unitgroup

import me.ezra_home.retail_software_solution.locations.model.UnitGroupEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UnitGroupRepository: JpaRepository<UnitGroupEntity, UUID>
