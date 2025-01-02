package me.ezra_home.retail_software_solution.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID
import me.ezra_home.retail_software_solution.model.util.TableNames

@Entity
@Table(name = TableNames.FRUIT)
data class Fruit(
        @Column(name = "name", nullable = false) 
        open val name: String,

        @Column(name = "alternate_name") 
        open val alternateName: String? = null,

        @Column(name = "color") 
        open val color: String? = null,

        @Column(name = "cost", precision = 10, scale = 2) 
        open val cost: Double? = null,

        @Column(name = "edible_ind", nullable = false) 
        open val edibleInd: Boolean,

) : AuditableEntity()
