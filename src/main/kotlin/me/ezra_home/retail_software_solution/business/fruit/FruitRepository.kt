package me.ezra_home.retail_software_solution.business.fruit

import jakarta.persistence.Entity
import me.ezra_home.retail_software_solution.model.entity.FruitEntity

@Entity(name = "FruitRepository")
open class FruitRepository : FruitEntity()