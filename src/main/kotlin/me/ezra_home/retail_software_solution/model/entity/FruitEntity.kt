package me.ezra_home.retail_software_solution.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.math.BigDecimal
import me.ezra_home.retail_software_solution.model.util.TableNames

@Entity
@Table(name = TableNames.FRUIT)
class FruitEntity (
        @Column(name = "name", nullable = false) 
        open var name: String,

        @Column(name = "alternate_name") 
        open var alternateName: String? = null,

        @Column(name = "color") 
        open var color: String? = null,

        @Column(name = "cost", precision = 10, scale = 2) 
        open var cost: BigDecimal? = null,

        @Column(name = "edible_ind", nullable = false) 
        open var edibleInd: Boolean,

) : AuditableEntity()
