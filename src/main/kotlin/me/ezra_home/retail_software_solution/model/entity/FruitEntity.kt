package me.ezra_home.retail_software_solution.model.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "fruit")
class FruitEntity(

        @Column(nullable = false)
        var name: String,

        @Column(name = "alternate_name")
        var alternateName: String? = null,

        @Column(nullable = false)
        var color: String,

        @Column(nullable = false)
        var cost: BigDecimal,

        @Column(name = "edible_ind", nullable = false)
        var edible: Boolean

) : AuditableEntity()
