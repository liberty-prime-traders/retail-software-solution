package me.ezra_home.retail_software_solution.organizations.business.unitvalue

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UnitValueRepository : JpaRepository<UnitValueEntity, UUID> {

    fun findByUnitGroupId(unitGroupId: UUID): Collection<UnitValueEntity>
}
