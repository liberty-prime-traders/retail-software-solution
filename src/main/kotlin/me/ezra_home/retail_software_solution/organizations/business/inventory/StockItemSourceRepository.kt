package me.ezra_home.retail_software_solution.organizations.business.inventory

import me.ezra_home.retail_software_solution.organizations.model.StockItemSourceEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface StockItemSourceRepository : JpaRepository<StockItemSourceEntity, UUID> {
  fun findByCode(code: StockItemSource): StockItemSourceEntity?
}
