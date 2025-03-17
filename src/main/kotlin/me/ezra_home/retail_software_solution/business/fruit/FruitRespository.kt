package me.ezra_home.retail_software_solution.repository

import me.ezra_home.retail_software_solution.model.entity.FruitEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FruitRepository : JpaRepository<FruitEntity, UUID> {
    fun findByName(name: String): FruitEntity?
}
