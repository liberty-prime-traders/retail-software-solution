package me.ezra_home.retail_software_solution.organizations.business.product

import me.ezra_home.retail_software_solution.organizations.model.ProductEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository: JpaRepository<ProductEntity, UUID> {
    @Query("SELECT p FROM ProductEntity p ORDER BY p.cursor ASC limit 50")
    fun findTopProducts(): Collection<ProductEntity>

    fun findFirstByProductNameIgnoreCase(productName: String): ProductEntity?

}
