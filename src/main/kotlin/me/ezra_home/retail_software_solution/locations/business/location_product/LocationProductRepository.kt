package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.locations.model.LocationProductEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LocationProductRepository : JpaRepository<LocationProductEntity, UUID> {
  fun findByProductId(productId: UUID): LocationProductEntity?
}
