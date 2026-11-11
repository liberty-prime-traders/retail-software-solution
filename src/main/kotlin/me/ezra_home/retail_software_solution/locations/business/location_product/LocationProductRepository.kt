package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.organizations.business.product.api.ProductStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LocationProductRepository : JpaRepository<LocationProductEntity, UUID> {
  @Query("SELECT lp FROM LocationProductEntity lp ORDER BY lower(lp.productName) ASC")
  fun findAllLocationProducts(): List<LocationProductEntity>

  fun findByOrgProductId(orgProductId: UUID): LocationProductEntity?

  fun findByReferenceNumberIn(referenceNumbers: Collection<String>): List<LocationProductEntity>

  @Query(
    "SELECT lp.id AS id, lp.referenceNumber AS referenceNumber, lp.orgProductId AS orgProductId, " +
      "lp.productName AS productName, lp.productGroupName AS productGroupName, lp.baseUnitId AS baseUnitId " +
      "FROM LocationProductEntity lp WHERE lp.id IN :ids"
  )
  fun findSummariesByIdIn(ids: Collection<UUID>): List<LocationProductSummaryView>

  @Query("SELECT lp.id FROM LocationProductEntity lp WHERE lp.id IN :ids AND lp.status <> :status")
  fun findIdsByIdInAndStatusNot(ids: Collection<UUID>, status: ProductStatus): List<UUID>

  @Query(
    "SELECT lp.id AS id, lp.orgProductId AS orgProductId, lp.baseUnitId AS baseUnitId " +
      "FROM LocationProductEntity lp WHERE lp.id IN :ids"
  )
  fun findIdentityViewsByIdIn(ids: Collection<UUID>): List<LocationProductIdentityView>

  @Query(
    "SELECT lp.id AS id, lp.orgProductId AS orgProductId, lp.baseUnitId AS baseUnitId " +
      "FROM LocationProductEntity lp WHERE lp.orgProductId IN :orgProductIds"
  )
  fun findIdentityViewsByOrgProductIdIn(orgProductIds: Collection<UUID>): List<LocationProductIdentityView>

  @Query(
    "SELECT lp.id AS id, lp.defaultSalePrice AS defaultSalePrice " +
      "FROM LocationProductEntity lp WHERE lp.id IN :ids"
  )
  fun findDefaultSalePriceViewsByIdIn(ids: Collection<UUID>): List<LocationProductDefaultSalePriceView>

  @Query(
    "SELECT lp.id AS id, lp.referenceNumber AS referenceNumber " +
      "FROM LocationProductEntity lp WHERE lp.referenceNumber IN :referenceNumbers"
  )
  fun findReferenceViewsByReferenceNumberIn(referenceNumbers: Collection<String>): List<LocationProductReferenceView>
}
