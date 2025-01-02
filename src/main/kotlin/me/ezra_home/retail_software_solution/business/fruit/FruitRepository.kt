package me.ezra_home.retail_software_solution.business.fruit

import me.ezra_home.retail_software_solution.model.entity.FruitEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface FruitRepository : JpaRepository<FruitEntity, UUID>