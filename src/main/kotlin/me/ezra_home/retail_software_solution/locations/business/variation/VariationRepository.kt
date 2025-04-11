package me.ezra_home.retail_software_solution.locations.business.variation

import me.ezra_home.retail_software_solution.locations.model.VariationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface VariationRepository : JpaRepository<VariationEntity, UUID>
