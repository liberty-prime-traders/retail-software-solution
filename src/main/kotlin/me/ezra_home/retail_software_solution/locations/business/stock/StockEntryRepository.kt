package me.ezra_home.retail_software_solution.locations.business.stock

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface StockEntryRepository : JpaRepository<StockEntryEntity, UUID> {

    @Query(
        "SELECT e FROM StockEntryEntity e " +
            "WHERE e.locationProductId IN :locationProductIds " +
            "AND e.quantityRemaining > 0 " +
            "ORDER BY e.locationProductId, e.createdOn ASC"
    )
    fun findFifoEntriesForProducts(locationProductIds: Collection<UUID>): List<StockEntryEntity>
}
