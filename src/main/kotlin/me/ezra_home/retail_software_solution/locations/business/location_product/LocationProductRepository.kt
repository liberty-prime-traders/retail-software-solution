package me.ezra_home.retail_software_solution.locations.business.location_product

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LocationProductRepository : JpaRepository<LocationProductEntity, UUID> {
  @Query("SELECT lp FROM LocationProductEntity lp ORDER BY lower(lp.productName) ASC")
  fun findAllLocationProducts(): List<LocationProductEntity>

  fun findByOrgProductId(orgProductId: UUID): LocationProductEntity?

  fun findByOrgProductIdIn(orgProductIds: Collection<UUID>): List<LocationProductEntity>
}
