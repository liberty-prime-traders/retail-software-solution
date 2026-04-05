package me.ezra_home.retail_software_solution.platform.business.tax_type

import me.ezra_home.retail_software_solution.platform.model.TaxTypeEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
internal interface TaxTypeRepository : JpaRepository<TaxTypeEntity, UUID>
