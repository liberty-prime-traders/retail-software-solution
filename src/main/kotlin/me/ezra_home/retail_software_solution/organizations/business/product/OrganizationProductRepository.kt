package me.ezra_home.retail_software_solution.organizations.business.product

import me.ezra_home.retail_software_solution.organizations.business.product.OrganizationProductEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface OrganizationProductRepository: JpaRepository<OrganizationProductEntity, UUID> {
    @Query("SELECT p FROM OrganizationProductEntity p ORDER BY lower(p.productName) ASC")
    fun findAllProducts(): List<OrganizationProductEntity>

    fun findFirstByProductNameIgnoreCase(productName: String): OrganizationProductEntity?

}
