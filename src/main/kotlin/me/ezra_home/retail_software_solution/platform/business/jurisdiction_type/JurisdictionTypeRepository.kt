package me.ezra_home.retail_software_solution.platform.business.jurisdiction_type

import me.ezra_home.retail_software_solution.platform.model.JurisdictionTypeEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
internal interface JurisdictionTypeRepository : JpaRepository<JurisdictionTypeEntity, UUID>
