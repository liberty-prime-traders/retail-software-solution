package me.ezra_home.retail_software_solution.organizations.business.product_tag

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ProductTagRepository : JpaRepository<ProductTagEntity, UUID> {

    @Query("""
        SELECT pt.tagId FROM ProductTagEntity pt
        WHERE pt.orgProductId = :orgProductId
        AND pt.endOn IS NULL
    """)
    fun findActiveTagIdsByOrgProductId(@Param("orgProductId") orgProductId: UUID): Set<UUID>

    @Query("""
        SELECT pt FROM ProductTagEntity pt
        WHERE pt.orgProductId = :orgProductId
        AND pt.endOn IS NULL
    """)
    fun findActiveProductTagsByOrgProductId(@Param("orgProductId") orgProductId: UUID): Collection<ProductTagEntity>

    @Query("""
        SELECT pt FROM ProductTagEntity pt
        WHERE pt.orgProductId IN :orgProductIds
        AND pt.endOn IS NULL
    """)
    fun findActiveProductTagsByOrgProductIds(@Param("orgProductIds") orgProductIds: Collection<UUID>): List<ProductTagEntity>
}
