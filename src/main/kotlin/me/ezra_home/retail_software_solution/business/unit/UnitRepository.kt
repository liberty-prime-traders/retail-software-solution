package me.ezra_home.retail_software_solution.business.unit

import java.util.UUID
import me.ezra_home.retail_software_solution.model.entity.UnitEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UnitRepository: JpaRepository<UnitEntity, UUID>