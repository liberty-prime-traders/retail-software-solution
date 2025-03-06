package me.ezra_home.retail_software_solution.business.variation

import me.ezra_home.retail_software_solution.model.entity.VariationEntity
import org.springframework.data.jpa.repository.JpaRepository

interface VariationRepository : JpaRepository<VariationEntity, Long> {
}