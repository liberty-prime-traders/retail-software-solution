package me.ezra_home.retail_software_solution.locations.business.purchase

import me.ezra_home.retail_software_solution.locations.model.PurchaseEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PurchaseRepository : JpaRepository<PurchaseEntity, UUID> {

  @Query("SELECT p FROM PurchaseEntity p")
  fun findTopN(pageable: Pageable): List<PurchaseEntity>
}
