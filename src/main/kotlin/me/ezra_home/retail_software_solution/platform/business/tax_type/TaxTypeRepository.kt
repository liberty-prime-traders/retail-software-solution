package me.ezra_home.retail_software_solution.platform.business.tax_type

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TaxTypeRepository : JpaRepository<TaxTypeEntity, UUID>
