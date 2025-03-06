package me.ezra_home.retail_software_solution.repository

import me.ezra_home.retail_software_solution.model.entity.FruitEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface FruitRepository : JpaRepository<FruitEntity, UUID> {
    fun findByName(name: String): FruitEntity?
}
