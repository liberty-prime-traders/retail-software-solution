package me.ezra_home.retail_software_solution.locations.business.category

import me.ezra_home.retail_software_solution.locations.model.CategoryEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CategoryRepository: JpaRepository<CategoryEntity, UUID>
