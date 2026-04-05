package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.locations.model.LocationProductEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
internal interface LocationProductRepository : JpaRepository<LocationProductEntity, UUID> {
  @Query("SELECT lp FROM LocationProductEntity lp ORDER BY lower(lp.productName) ASC")
  fun findAllLocationProducts(): List<LocationProductEntity>

  fun findByProductId(productId: UUID): LocationProductEntity?
}
