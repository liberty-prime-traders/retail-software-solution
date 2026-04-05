package me.ezra_home.retail_software_solution.organizations.business.product_tag

import me.ezra_home.retail_software_solution.organizations.business.product_tag.ProductTagEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ProductTagRepository : JpaRepository<ProductTagEntity, UUID> {

    @Query("""
        SELECT pt.tagId FROM ProductTagEntity pt
        WHERE pt.productId = :productId
        AND pt.endOn IS NULL
    """)
    fun findActiveTagIdsByProductId(@Param("productId") productId: UUID): Set<UUID>

    @Query("""
        SELECT pt FROM ProductTagEntity pt
        WHERE pt.productId = :productId
        AND pt.endOn IS NULL
    """)
    fun findActiveProductTagsByProductId(@Param("productId") productId: UUID): Collection<ProductTagEntity>

    @Query("""
        SELECT pt FROM ProductTagEntity pt
        WHERE pt.productId IN :productIds
        AND pt.endOn IS NULL
    """)
    fun findActiveProductTagsByProductIds(@Param("productIds") productIds: Collection<UUID>): List<ProductTagEntity>
}
