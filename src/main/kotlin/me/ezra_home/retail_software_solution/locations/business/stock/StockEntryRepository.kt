package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.locations.model.StockEntryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.UUID

@Repository
interface StockEntryRepository : JpaRepository<StockEntryEntity, UUID> {
  @Query("SELECT COALESCE(SUM(e.quantityRemaining), 0) FROM StockEntryEntity e WHERE e.locationProductId = :locationProductId")
  fun sumQuantityRemainingByLocationProductId(locationProductId: UUID): BigDecimal
}
