package me.ezra_home.retail_software_solution.organizations.business.unitconversion

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UnitConversionRepository : JpaRepository<UnitConversionEntity, UUID> {
    fun existsByFromUnitIdAndToUnitId(fromUnitId: UUID, toUnitId: UUID): Boolean
}
