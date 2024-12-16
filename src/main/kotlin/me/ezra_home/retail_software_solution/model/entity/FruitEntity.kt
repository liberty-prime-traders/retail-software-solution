package me.ezra_home.retail_software_solution.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import me.ezra_home.retail_software_solution.model.util.TableNames
import java.math.BigDecimal

@Entity
@Table(name = TableNames.FRUIT)
class FruitEntity: AuditableEntity() {

    @Size(max = 255)
    @NotNull
    @Column(name = "name", nullable = false)
    var name: String? = null

    @Size(max = 255)
    @Column(name = "alternate_name")
    var alternateName: String? = null

    @Size(max = 50)
    @NotNull
    @Column(name = "color", nullable = false, length = 50)
    var color: String? = null

    @NotNull
    @Column(name = "cost", nullable = false, precision = 10, scale = 2)
    var cost: BigDecimal? = null

    @NotNull
    @Column(name = "edible_ind", nullable = false)
    var edibleInd: Boolean? = false
}