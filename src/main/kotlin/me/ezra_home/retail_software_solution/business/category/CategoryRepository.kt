package me.ezra_home.retail_software_solution.business.category

import java.util.UUID
import me.ezra_home.retail_software_solution.model.entity.CategoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CategoryRepository: JpaRepository<CategoryEntity, UUID>