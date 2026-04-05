package me.ezra_home.retail_software_solution.organizations.business.product_category

import me.ezra_home.retail_software_solution.organizations.model.ProductCategoryEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
internal interface ProductCategoryRepository: JpaRepository<ProductCategoryEntity, UUID>
