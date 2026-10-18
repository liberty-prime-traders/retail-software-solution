package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface StockMovementRepository : JpaRepository<StockMovementEntity, UUID> {

  fun findByDocumentReferenceAndMovementType(
    documentReference: String,
    movementType: MovementType
  ): List<StockMovementEntity>

  @Query("SELECT DISTINCT sm.detailReference FROM StockMovementEntity sm WHERE sm.documentReference = :documentReference AND sm.detailReference IN :detailReferences AND sm.movementType = :movementType")
  fun findPresentDetailReferences(documentReference: String, detailReferences: Collection<String>, movementType: MovementType): Set<String>

  fun findByLocationProductId(locationProductId: UUID, pageable: Pageable): List<StockMovementEntity>
}
