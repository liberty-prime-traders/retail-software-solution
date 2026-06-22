package me.ezra_home.retail_software_solution.organizations.business.product_tax_assignment

import me.ezra_home.retail_software_solution.organizations.model.ProductTaxAssignmentEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface ProductTaxAssignmentRepository : JpaRepository<ProductTaxAssignmentEntity, UUID> {

    fun findByProductId(productId: UUID): List<ProductTaxAssignmentEntity>

    @Query("""
        SELECT a FROM ProductTaxAssignmentEntity a
        JOIN TaxRateEntity r ON r.id = a.taxRateId
        WHERE a.productId = :productId
          AND r.orgJurisdictionTaxTypeId = :orgJurisdictionTaxTypeId
          AND (a.endDate IS NULL OR a.endDate >= :from)
          AND (:to IS NULL OR a.startDate <= :to)
          AND (:excludeId IS NULL OR a.id <> :excludeId)
    """)
    fun findOverlapping(
        @Param("productId") productId: UUID,
        @Param("orgJurisdictionTaxTypeId") orgJurisdictionTaxTypeId: UUID,
        @Param("from") from: LocalDate,
        @Param("to") to: LocalDate?,
        @Param("excludeId") excludeId: UUID?
    ): List<ProductTaxAssignmentEntity>
}
