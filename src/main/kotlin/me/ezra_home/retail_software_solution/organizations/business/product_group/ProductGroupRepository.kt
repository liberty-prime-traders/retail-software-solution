package me.ezra_home.retail_software_solution.organizations.business.product_group

import me.ezra_home.retail_software_solution.organizations.model.ProductGroupEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
internal interface ProductGroupRepository : JpaRepository<ProductGroupEntity, UUID>
