package me.ezra_home.retail_software_solution.organizations.business.product_group

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ProductGroupRepository : JpaRepository<ProductGroupEntity, UUID>
